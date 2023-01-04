package io.github.mcsim4s.dt.dao.impl

import com.zaxxer.hikari._
import doobie._
import doobie.free.connection
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.circe.jsonb.implicits._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.mcsim4s.dt.dao.{LogDoobieQueries, TaskDao}
import io.github.mcsim4s.dt.dao.impl.LiveTaskDao._
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.UnexpectedDbError
import io.github.mcsim4s.dt.model.query.DeepTraceTaskStateFilter
import io.github.mcsim4s.dt.model.task.{DeepTraceTask, TaskFilter}
import zio._
import zio.interop.catz._
import zio.stream.interop.fs2z._

import java.util.UUID

case class LiveTaskDao(xa: Transactor[Task]) extends TaskDao with LogDoobieQueries {
  import columns._

  override def create(task: DeepTraceTask)(implicit trace: Trace): IO[UnexpectedDbError, Unit] = {
    sql"""INSERT INTO $TableName
         values(${task.id}, ${task.state}, $task)""".update.run
      .transact(xa)
      .mapError(err => UnexpectedDbError("insert", err))
      .unit
  }

  override def list(filter: TaskFilter)(implicit trace: Trace): stream.Stream[UnexpectedDbError, DeepTraceTask] = {
    selectQuery(filter).stream
      .transact(xa)
      .toZStream()
      .mapError(err => UnexpectedDbError("list", err))
  }

  override def updateCas(
      old: DeepTraceTask,
      update: DeepTraceTask
    )(implicit trace: Trace): IO[DeepTraceError, DeepTraceTask] =
    (for {
      existingOpt <- selectQuery(TaskFilter(taskId = Some(old.id))).to[List]
      existing <- existingOpt.headOption match {
        case Some(value) => connection.pure(value)
        case None => connection.raiseError(NotFoundException(old.id))
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
        case error => UnexpectedDbError("cas_update", error)
      }

  private def selectQuery(filter: TaskFilter): doobie.Query0[DeepTraceTask] = {
    val idFragment = filter.taskId match {
      case Some(value) => fr"AND $id = $value"
      case None => fr""
    }
    sql"""SELECT $json FROM $TableName
         WHERE 1 = 1
         $idFragment"""
      .query[DeepTraceTask]
  }

  private def updateQuery(task: DeepTraceTask): doobie.Update0 = {
    sql"""UPDATE $TableName SET ($state, $json) = (${task.state}, $task)""".update
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
}

object LiveTaskDao {
  case class NotFoundException(id: UUID) extends Throwable
  case class ConflictException(id: UUID) extends Throwable

  private val TableName = fr"tasks"

  object columns {
    val id = fr"id"
    val state = fr"state"
    val json = fr"json"
  }

  implicit val taskRead: Read[DeepTraceTask] = Read[Json].map { json =>
    json.as[DeepTraceTask].getOrElse(throw new IllegalStateException(s"Wrong task format '$json'"))
  }

  implicit val taskWrite: Write[DeepTraceTask] = Write[Json].contramap { task =>
    task.asJson
  }

  implicit val taskStateWrite: Write[DeepTraceTask.State] = Write[String].contramap { state =>
    val stateEnum = state match {
      case DeepTraceTask.New => DeepTraceTaskStateFilter.NEW
      case _: DeepTraceTask.Fetching => DeepTraceTaskStateFilter.FETCHING
      case DeepTraceTask.Clustering => DeepTraceTaskStateFilter.CLUSTERING
      case _: DeepTraceTask.ClustersBuilt => DeepTraceTaskStateFilter.CLUSTERS_BUILT
    }
    stateEnum.toString()
  }

  case class TaskDaoConf(
      url: String,
      user: String,
      password: String)

  private def transactor(conf: TaskDaoConf): ZIO[Scope, Throwable, Transactor[Task]] = {
    for {
      ds <- ZIO
        .fromAutoCloseable {
          ZIO.from {
            import conf._
            val hikariConfig = new HikariConfig()
            hikariConfig.setPoolName(s"db-tasks")
            hikariConfig.setDriverClassName("org.postgresql.Driver")
            hikariConfig.setJdbcUrl(url)
            hikariConfig.setReadOnly(false)
            hikariConfig.setUsername(user)
            hikariConfig.setPassword(password)
            new HikariDataSource(hikariConfig)
          }
        }
      ec <- ZIO.blockingExecutor
    } yield Transactor.fromDataSource.apply(ds, ec.asExecutionContext)
  }

  val layer: ZLayer[TaskDaoConf with Scope, Throwable, TaskDao] = ZLayer {
    for {
      config <- ZIO.service[TaskDaoConf]
      tx <- transactor(config)
      dao = LiveTaskDao(tx)
      _ <- dao.init().mapError(_.cause)
    } yield dao
  }
}
