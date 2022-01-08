package io.github.mcsim4s.dt

import AnalysisRequest.RawTraceSource
import TraceCluster.ClusterSource
import mappers.RawTraceMappers

import zio.stream.ZStream
import zio.{IO, UIO, ZIO}

case class LiveEngine() extends Engine.Service {
  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      clusters <- cluster(request.traceSource)
      _ <- clusters.map(processCluster).runDrain
    } yield AnalysisReport(id = "")
  }

  private def cluster(traceSource: RawTraceSource): IO[DeepTraceError, ClusterSource] =
    traceSource
      .mapM(RawTraceMappers.fromRaw)
      .runCollect
      .map(chunk => ZStream.succeed(TraceCluster("", ZStream.fromChunk(chunk))))

  private def processCluster(cluster: TraceCluster): UIO[Unit] =
    ZIO.unit
}
