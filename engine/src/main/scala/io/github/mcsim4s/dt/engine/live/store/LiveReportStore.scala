package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.live.store.LiveReportStore.CasRetryPolicy
import io.github.mcsim4s.dt.engine.store.ReportStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.model.DeepTraceError.{CasConflict, ReportNotFound}
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError}
import zio._
import zio.clock.Clock
import zio.random.Random
import zio.stm._

class LiveReportStore(reportsRef: TMap[String, AnalysisReport], random: Random.Service, clock: Clock.Service)
    extends ReportStore.Service {

  override def create(request: AnalysisRequest): UIO[AnalysisReport] =
    for {
      id <- random.nextUUID.map(_.toString)
      now <- clock.instant
      report = AnalysisReport(
        id = id,
        createdAt = now,
        service = request.service,
        operation = request.operation,
        state = AnalysisReport.ClustersBuilt(Seq(ClusterId("test", "test")))
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
    } yield result).retry(CasRetryPolicy).provide(Has(clock))

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
  private val CasRetryPolicy: Schedule[Any, DeepTraceError, (DeepTraceError, Long)] =
    Schedule.recurWhile[DeepTraceError] {
      case _: CasConflict => true
      case _              => false
    } && Schedule.recurs(3)

  def makeService: ZIO[Random with Clock, Nothing, LiveReportStore] =
    for {
      reportsRef <- STM.atomically(TMap.make[String, AnalysisReport]())
      random <- ZIO.service[Random.Service]
      clock <- ZIO.service[Clock.Service]
    } yield new LiveReportStore(reportsRef, random, clock)

  val layer: ZLayer[Random with Clock, Nothing, ReportStore] = makeService.toLayer
}
