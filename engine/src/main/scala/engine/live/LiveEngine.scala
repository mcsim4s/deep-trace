package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.model.TraceCluster.ClusterSource
import io.github.mcsim4s.dt.model.{DeepTraceError, TraceCluster}
import io.github.mcsim4s.dt.model.mappers.RawTraceMappers
import io.github.mcsim4s.dt.engine.{AnalysisReport, AnalysisRequest, Engine}
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore}
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
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
