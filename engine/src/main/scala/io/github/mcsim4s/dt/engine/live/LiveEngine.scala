package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.TraceParser.TraceParser
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore, SpanStore}
import io.github.mcsim4s.dt.engine.{Engine, TraceParser}
import io.github.mcsim4s.dt.model.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model._
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import zio._
import zio.console.Console
import zio.random.Random

import scala.concurrent.duration.Duration

class LiveEngine(
    reportStore: ReportStore.Service,
    clusterStore: ClusterStore.Service,
    processStore: ProcessStore.Service,
    traceParser: TraceParser.Service,
    spanStore: SpanStore.Service,
    console: Console.Service,
    random: Random.Service
) extends Engine.Service {

  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      report <- reportStore.create(request)
      _ <- parseTraces(report, request)
        .flatMap { clusters =>
          ZIO.foreach_(clusters)(processCluster) *>
            reportStore.update(report.id) { _ =>
              ZIO.succeed(report.copy(state = AnalysisReport.ClustersBuilt(clusters.map(_.id))))
            }
        }
        .tapBoth(
          err => console.putStrLnErr(s"Async clustering error. Err: $err").ignore,
          _ => console.putStrLn("Async clustering complete").ignore
        )
        .forkDaemon
    } yield report
  }

  private def parseTraces(report: AnalysisReport, request: AnalysisRequest): IO[DeepTraceError, Seq[TraceCluster]] =
    request.traceSource
      .tap(trace => console.putStrLn(s"Parsing another trace with ${trace.spans.size} spans").ignore)
      .flatMap(trace => traceParser.parse(trace, request.operation))
      .foreach(root => clusterStore.getOrCreate(report.id, root)) *>
      clusterStore
        .list(report.id)
        .runCollect
        .map(_.toList)

  private def processCluster(cluster: TraceCluster): IO[DeepTraceError, TraceCluster] =
    clusterStore
      .update(cluster.id) { old =>
        avgTrace(old.root)
          .map { trace =>
            old.copy(stats = Some(trace))
          }
      }

  def avgTrace(process: Process): IO[DeepTraceError, ClusterStats] =
    for {
      spans <- spanStore.list(process.id)
      (avgStart, avgDuration) = {
        val start = new DescriptiveStatistics()
        val duration = new DescriptiveStatistics()
        spans.foreach { span =>
          start.addValue(span.getStartTime.toInstant.toDuration.toNanos)
          duration.addValue(span.getDuration.asScala.toNanos)
        }
        Duration.fromNanos(start.getMean) -> Duration.fromNanos(duration.getMean)
      }
      stats = ProcessStats(avgStart, avgDuration, spans.map(_.getDuration.asScala))
      children <-
        if (process.children.nonEmpty) {
          ZIO
            .foreach(process.children)(avgTrace)
            .map(_.reduce(_ ++ _))
        } else IO.succeed(ClusterStats.empty)

    } yield ClusterStats(Map(process.id.hash -> stats)) ++ children
}

object LiveEngine {
  def makeService: ZIO[
    ReportStore with ClusterStore with ProcessStore with TraceParser with SpanStore with Console with Random,
    Nothing,
    LiveEngine
  ] =
    for {
      reportStore <- ZIO.service[ReportStore.Service]
      clusterStore <- ZIO.service[ClusterStore.Service]
      processStore <- ZIO.service[ProcessStore.Service]
      traceParser <- ZIO.service[TraceParser.Service]
      spanStore <- ZIO.service[SpanStore.Service]
      console <- ZIO.service[Console.Service]
      random <- ZIO.service[Random.Service]
    } yield new LiveEngine(reportStore, clusterStore, processStore, traceParser, spanStore, console, random)

  val layer: ZLayer[
    ReportStore with ClusterStore with ProcessStore with TraceParser with SpanStore with Console with Random,
    Nothing,
    Engine
  ] = makeService.toLayer
}
