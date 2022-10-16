package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.store.ReportStore
import io.github.mcsim4s.dt.model.DeepTraceError.{CasConflict, ReportNotFound}
import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError}
import zio._
import zio.Clock
import zio.stm._
import zio.Random

class LiveReportStore(reportsRef: TMap[String, AnalysisReport]) extends ReportStore {

  override def create(request: AnalysisRequest): UIO[AnalysisReport] =
    for {
      id <- Random.nextUUID.map(_.toString)
      now <- Clock.instant
      report = AnalysisReport(
        id = id,
        createdAt = now,
        service = request.service,
        operation = request.operation,
        state = AnalysisReport.Clustering
      )
      _ <- STM.atomically(reportsRef.put(id, report))
    } yield report

  override def list(): UIO[List[AnalysisReport]] = STM.atomically(reportsRef.values)

  override def update(reportId: String)(
      upd: AnalysisReport => IO[DeepTraceError, AnalysisReport]
  ): IO[DeepTraceError, AnalysisReport] =
    (for {
      old <- get(reportId)
      result <- upd(old)
      _ <- updateCas(old, result)
    } yield result).retry(CasRetryPolicy)

  private def updateCas(from: AnalysisReport, to: AnalysisReport): IO[DeepTraceError, AnalysisReport] =
    STM.atomically {
      for {
        old <- reportsRef.get(from.id).flatMap(opt => STM.fromOption(opt).orElseFail(ReportNotFound(from.id)))
        _ <- STM.fail(CasConflict("Analysis report", from.id)).when(old != from)
        _ <- reportsRef.put(to.id, to)
      } yield to
    }

  def get(reportId: String): IO[ReportNotFound, AnalysisReport] =
    STM
      .atomically(reportsRef.get(reportId))
      .flatMap(r => ZIO.fromOption(r))
      .orElseFail(ReportNotFound(reportId))
}

object LiveReportStore {
  def makeService: ZIO[Any, Nothing, LiveReportStore] =
    for {
      reportsRef <- STM.atomically(TMap.make[String, AnalysisReport]())
    } yield new LiveReportStore(reportsRef)

  val layer: ZLayer[Any, Nothing, ReportStore] = ZLayer(makeService)
}
