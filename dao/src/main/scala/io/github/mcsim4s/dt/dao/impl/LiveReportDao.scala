package io.github.mcsim4s.dt.dao.impl

import cats.data.NonEmptyList
import com.zaxxer.hikari._
import doobie._
import doobie.free.connection
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.circe.jsonb.implicits._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.mcsim4s.dt.dao.{LogDoobieQueries, ReportDao}
import io.github.mcsim4s.dt.dao.impl.LiveReportDao._
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.UnexpectedDbError
import io.github.mcsim4s.dt.model.query.ReportStateName
import io.github.mcsim4s.dt.model.report.{Report, ReportFilter}
import io.github.mcsim4s.toolkit.config.{JdbcConfig, Pureconfig}
import io.github.mcsim4s.toolkit.doobie.OpsTransactor
import zio._
import zio.interop.catz._
import zio.stream.interop.fs2z._
import zio.telemetry.opentelemetry.Tracing
import scalapb_circe.codec._

import java.util.UUID

case class LiveReportDao(xa: Transactor[Task]) extends ReportDao with LogDoobieQueries {
  import columns._

  override def create(task: Report)(implicit trace: Trace): IO[UnexpectedDbError, Unit] = {
    sql"""INSERT INTO $TableName
         values(${task.id}, ${task.state}, $task)""".update.run
      .transact(xa)
      .mapError(err => UnexpectedDbError("insert", err))
      .unit
  }

  override def list(filter: ReportFilter)(implicit trace: Trace): stream.Stream[UnexpectedDbError, Report] = {
    selectQuery(filter).stream
      .transact(xa)
      .toZStream()
      .mapError(err => UnexpectedDbError("list", err))
  }

  override def updateCas(
      old: Report,
      update: Report
  )(implicit trace: Trace): IO[DeepTraceError, Report] =
    (for {
      existingOpt <- selectQuery(ReportFilter(taskId = Some(old.id))).to[List]
      existing <- existingOpt.headOption match {
        case Some(value) => connection.pure(value)
        case None        => connection.raiseError(NotFoundException(old.id))
      }
      _ <-
        if (old != existing) {
          connection.raiseError(ConflictException(old.id))
        } else connection.unit
      _ <- updateQuery(update).run
    } yield update)
      .transact(xa)
      .mapError {
        case NotFoundException(id) => DeepTraceError.DeepTraceTaskNotFound(id)
        case ConflictException(id) => DeepTraceError.CasConflict("deep_trace_task", id.toString)
        case error                 => UnexpectedDbError("cas_update", error)
      }

  private def selectQuery(filter: ReportFilter): doobie.Query0[Report] = {
    val idFragment = filter.taskId match {
      case Some(value) => fr"AND $id = $value"
      case None        => fr""
    }
    val stateFragment = NonEmptyList.fromList(filter.state.toList) match {
      case Some(value) => fr"AND " ++ Fragments.in(fr"$state", value)
      case None        => fr""
    }

    sql"""SELECT $json FROM $TableName
         WHERE 1 = 1
         $idFragment
         $stateFragment"""
      .query[Report]
  }

  private def updateQuery(task: Report): doobie.Update0 = {
    sql"""UPDATE $TableName
         SET ($state, $json) = (${task.state}, $task)
         WHERE $id = ${task.id}""".update
  }

  private def init(): ZIO[Any, UnexpectedDbError, Unit] = {
    sql"""
       CREATE TABLE IF NOT EXISTS $TableName (
          $id UUID not null,
          $state character(32),
          $json jsonb,
          CONSTRAINT tasks_pk PRIMARY KEY ($id)
       )""".update.run
      .transact(xa)
      .mapError(err => UnexpectedDbError("init", err))
      .unit
  }

  private def clear(): ZIO[Any, UnexpectedDbError, Unit] = {
    sql"""
         DELETE FROM $TableName;
       """.update.run
      .transact(xa)
      .mapError(err => UnexpectedDbError("init", err))
      .unit
  }
}

object LiveReportDao {
  case class NotFoundException(id: UUID) extends Throwable
  case class ConflictException(id: UUID) extends Throwable

  private val TableName = fr"tasks"

  object columns {
    val id = fr"id"
    val state = fr"state"
    val json = fr"json"
  }

  implicit val taskRead: Read[Report] = Read[Json].map { json =>
    json.as[Report].getOrElse(throw new IllegalStateException(s"Wrong task format '$json'"))
  }

  implicit val taskWrite: Write[Report] = Write[Json].contramap { task =>
    task.asJson
  }

  implicit val stateNamePut: Put[ReportStateName] = Put[String].contramap(_.toString())

  implicit val taskStateWrite: Write[Report.State] = Write[String].contramap { state =>
    val stateEnum = state match {
      case Report.New              => ReportStateName.NEW
      case _: Report.Fetching      => ReportStateName.FETCHING
      case Report.Clustering       => ReportStateName.CLUSTERING
      case _: Report.ClustersBuilt => ReportStateName.CLUSTERS_BUILT
    }
    stateEnum.toString()
  }

  val layer: ZLayer[Tracing with Scope, Throwable, ReportDao] = ZLayer {
    for {
      config <- Pureconfig.load[JdbcConfig]("tasks-dao")
      tracing <- ZIO.service[Tracing]
      transactor <- OpsTransactor.makeTransactor(config, tracing)
      dao = LiveReportDao(transactor)
      _ <- dao.init().mapError(_.cause)
      _ <- dao.clear().mapError(_.cause)
    } yield dao
  }
}
