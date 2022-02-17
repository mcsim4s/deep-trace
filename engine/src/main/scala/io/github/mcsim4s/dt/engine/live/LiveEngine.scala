package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore, SpanStore}
import io.github.mcsim4s.dt.model.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model.mappers.RawTraceMappers
import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError, TraceCluster}
import zio._
import zio.console.Console
import zio.random.Random

class LiveEngine(
    reportStore: ReportStore.Service,
    clusterStore: ClusterStore.Service,
    processStore: ProcessStore.Service,
    spanStore: SpanStore.Service,
    console: Console.Service,
    random: Random.Service
) extends Engine.Service {

  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      report <- reportStore.create(request)
      _ <- parseTraces(report, request.traceSource).flatMap { clusters =>
        reportStore.update(report.id) { _ =>
          ZIO.succeed(report.copy(state = AnalysisReport.ClustersBuilt(clusters.map(_.id))))
        }
      }.forkDaemon
    } yield report
  }

  private def parseTraces(report: AnalysisReport, traceSource: RawTraceSource): IO[DeepTraceError, Seq[TraceCluster]] =
    traceSource
      .tap(trace => console.putStrLn(s"Parsing another trace with ${trace.spans.size} spans").ignore)
      .mapM(RawTraceMappers.fromRaw)
      .provide(Has(random))
      .foreach { process =>
        val id = ClusterId(report.id, process.hash)
        clusterStore.getOrCreate(id) *>
          clusterStore.update(id)(old => ZIO.succeed(old.copy(avgProcess = Some(process))))
      } *> clusterStore.list(report.id).runCollect.map(_.toList)

  private def processCluster(cluster: TraceCluster): UIO[Unit] =
    ZIO.unit
}

object LiveEngine {
  def makeService: ZIO[
    ReportStore with ClusterStore with ProcessStore with SpanStore with Console with Random,
    Nothing,
    LiveEngine
  ] =
    for {
      reportStore <- ZIO.service[ReportStore.Service]
      clusterStore <- ZIO.service[ClusterStore.Service]
      processStore <- ZIO.service[ProcessStore.Service]
      spanStore <- ZIO.service[SpanStore.Service]
      console <- ZIO.service[Console.Service]
      random <- ZIO.service[Random.Service]
    } yield new LiveEngine(reportStore, clusterStore, processStore, spanStore, console, random)

  val layer: ZLayer[
    ReportStore with ClusterStore with ProcessStore with SpanStore with Console with Random,
    Nothing,
    Engine
  ] = makeService.toLayer
}
