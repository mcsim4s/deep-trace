package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.dao.ReportDao
import io.github.mcsim4s.dt.engine.store.ReportStore
import io.github.mcsim4s.dt.model.DeepTraceError.DeepTraceTaskNotFound
import io.github.mcsim4s.dt.model.report._
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._
import zio.stream._

import java.util.UUID

class LiveReportStore(taskDao: ReportDao) extends ReportStore {

  override def create(request: AnalysisRequest)(implicit trace: Trace): IO[DeepTraceError, Report] =
    for {
      id <- Random.nextUUID
      now <- Clock.instant
      task = Report(
        id = id,
        createdAt = now,
        query = request.query,
        state = Report.New
      )
      _ <- taskDao.create(task)
    } yield task

  override def list(filter: ReportFilter = ReportFilter.Any)(implicit trace: Trace): Stream[DeepTraceError, Report] =
    taskDao.list(filter)

  override def update(
      id: UUID
  )(upd: Report => IO[DeepTraceError, Report])(implicit trace: Trace): IO[DeepTraceError, Report] =
    (for {
      old <- get(id)
      result <- upd(old)
      _ <- taskDao.updateCas(old, result)
    } yield result).retry(CasRetryPolicy)

  def get(id: UUID)(implicit trace: Trace): IO[DeepTraceTaskNotFound, Report] = {
    taskDao
      .list(filter = ReportFilter(taskId = Some(id)))
      .runHead
      .flatMap(r => ZIO.fromOption(r))
      .orElseFail(DeepTraceTaskNotFound(id))
  }
}

object LiveReportStore {

  val layer: ZLayer[ReportDao, Nothing, ReportStore] = ZLayer {
    for {
      dao <- ZIO.service[ReportDao]
    } yield new LiveReportStore(dao)
  }
}
