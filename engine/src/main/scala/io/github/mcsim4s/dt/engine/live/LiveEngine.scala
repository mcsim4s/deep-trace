package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore, SpanStore}
import io.github.mcsim4s.dt.model.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import io.github.mcsim4s.dt.model.mappers.RawTraceMappers
import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError, TraceCluster}
import zio.{IO, UIO, ZIO, ZLayer}

class LiveEngine(
    reportStore: ReportStore.Service,
    clusterStore: ClusterStore.Service,
    processStore: ProcessStore.Service,
    spanStore: SpanStore.Service
) extends Engine.Service {

  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      report <- reportStore.create(request)
      _ <- parseTraces(report, request.traceSource).flatMap { clusters =>
        reportStore.update(report.id) { _ =>
          ZIO.succeed(report.copy(state = AnalysisReport.ClustersBuilt(clusters.map(_.id))))
        }
      }.fork
    } yield report
  }

  private def parseTraces(report: AnalysisReport, traceSource: RawTraceSource): IO[DeepTraceError, Seq[TraceCluster]] =
    traceSource
      .mapM(RawTraceMappers.fromRaw)
      .foreach { process =>
        clusterStore.getOrCreate(report.id, process)
      } *> clusterStore.read(report.id).runCollect.map(_.toSeq)

  private def processCluster(cluster: TraceCluster): UIO[Unit] =
    ZIO.unit
}

object LiveEngine {
  def makeService: ZIO[ReportStore with ClusterStore with ProcessStore with SpanStore, Nothing, LiveEngine] =
    for {
      reportStore <- ZIO.service[ReportStore.Service]
      clusterStore <- ZIO.service[ClusterStore.Service]
      processStore <- ZIO.service[ProcessStore.Service]
      spanStore <- ZIO.service[SpanStore.Service]
    } yield new LiveEngine(reportStore, clusterStore, processStore, spanStore)

  val layer: ZLayer[ReportStore with ClusterStore with ProcessStore with SpanStore, Nothing, Engine] =
    makeService.toLayer
}
