package io.github.mcsim4s.dt.live

import io.github.mcsim4s.dt.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.{AnalysisReport, AnalysisRequest, DeepTraceError, Engine, TraceCluster}
import io.github.mcsim4s.dt.Engine.Engine
import io.github.mcsim4s.dt.TraceCluster.ClusterSource
import io.github.mcsim4s.dt.mappers.RawTraceMappers
import io.github.mcsim4s.dt.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.store.{ClusterStore, ProcessStore, ReportStore}
import io.github.mcsim4s.dt.store.ReportStore.ReportStore
import zio.{IO, UIO, ZIO, ZLayer}

class LiveEngine(
    reportStore: ReportStore.Service,
    clusterStore: ClusterStore.Service,
    processStore: ProcessStore.Service
) extends Engine.Service {
  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      report <- reportStore.create(request)
      clusters <- parseTraces(report, request.traceSource)
      _ <- clusters.map(processCluster).runDrain
    } yield report
  }

  private def parseTraces(report: AnalysisReport, traceSource: RawTraceSource): IO[DeepTraceError, ClusterSource] =
    traceSource
      .mapM(RawTraceMappers.fromRaw)
      .foreach { trace =>
        for {
          cluster <- clusterStore.getOrCreate(report.id, trace.hash)
        } yield ()
      }
      .as(clusterStore.read(report.id))

  private def processCluster(cluster: TraceCluster): UIO[Unit] =
    ZIO.unit
}

object LiveEngine {
  def makeService: ZIO[ReportStore with ClusterStore with ProcessStore, Nothing, LiveEngine] =
    for {
      reportStore <- ZIO.service[ReportStore.Service]
      clusterStore <- ZIO.service[ClusterStore.Service]
      processStore <- ZIO.service[ProcessStore.Service]
    } yield new LiveEngine(reportStore, clusterStore, processStore)

  val layer: ZLayer[ReportStore with ClusterStore with ProcessStore, Nothing, Engine] = makeService.toLayer
}
