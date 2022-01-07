package io.github.mcsim4s.dt

import AnalysisRequest.RawTraceSource
import TraceCluster.ClusterSource

import zio.{UIO, ZIO}
import zio.stream.ZStream

case class LiveEngine() extends Engine.Service {
  override def process(request: AnalysisRequest): UIO[AnalysisReport] = {
    for {
      clusters <- cluster(request.traceSource)
      _ <- clusters.map(processCluster).runDrain
    } yield AnalysisReport(id = "")
  }

  private def cluster(traceSource: RawTraceSource): UIO[ClusterSource] =
    ZIO.succeed(
      ZStream.succeed(TraceCluster("", traceSource.mapM(Trace.fromRaw)))
    )

  private def processCluster(cluster: TraceCluster): UIO[Unit] =
    ZIO.unit
}
