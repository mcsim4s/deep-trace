package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.TraceParser.TraceParser
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore}
import io.github.mcsim4s.dt.engine.{Engine, TraceParser}
import io.github.mcsim4s.dt.model.Process.ProcessId
import io.github.mcsim4s.dt.model.ProcessStats.DurationStats
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model._
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import zio._
import zio.console.Console
import zio.random.Random

import scala.concurrent.duration.Duration

class LiveEngine(
    reportStore: ReportStore.Service,
    clusterStore: ClusterStore.Service,
    traceParser: TraceParser.Service,
    processStore: ProcessStore.Service,
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
      .foreach(root => {
        val debug = root.instances.groupBy(_.processId)
        console.putStrLn(debug.toString()).ignore *>
          clusterStore
            .getOrCreate(report.id, root.process)
            .flatMap(cluster => ZIO.foreach(root.instances)(i => processStore.add(cluster.id, i)))
      }) *>
      clusterStore
        .list(report.id)
        .runCollect
        .map(_.toList)

  private def processCluster(cluster: TraceCluster): IO[DeepTraceError, TraceCluster] =
    clusterStore
      .update(cluster.id) { old =>
        computeStats(old)
          .map { stats =>
            old.copy(stats = Some(stats))
          }
      }

  def computeStats(cluster: TraceCluster): IO[DeepTraceError, ClusterStats] =
    for {
      processesStats <- processesStatsRecursive(cluster.id)(cluster.root)
      rootSpans <- processStore.list(cluster.id, cluster.root.id)
    } yield ClusterStats(
      traceCount = rootSpans.size,
      containsErrors = false,
      processes = processesStats
    )

  def processesStatsRecursive(
      clusterId: ClusterId
  )(process: Process): IO[DeepTraceError, Map[ProcessId, ProcessStats]] =
    for {
      currentStats <- singleProcessStats(clusterId)(process)
      subProcesses = process match {
        case Process.SequentialProcess(children)     => children
        case Process.ParallelProcess(_, _, children) => children
        case Process.ConcurrentProcess(of)           => Seq(of)
        case Process.Gap(_)                          => Seq.empty
      }
      children <-
        if (subProcesses.nonEmpty) {
          ZIO
            .foreach(subProcesses)(processesStatsRecursive(clusterId))
            .map(_.reduce(_ ++ _))
        } else IO.succeed(Map.empty)
    } yield Map(process.id -> currentStats) ++ children

  def singleProcessStats(clusterId: ClusterId)(process: Process): IO[DeepTraceError, ProcessStats] =
    for {
      spans <- processStore.list(clusterId, process.id)
      duration <- ZIO.effectTotal {
        val duration = new DescriptiveStatistics()
        spans.foreach { span =>
          duration.addValue(span.duration.toNanos)
        }
        DurationStats(Duration.fromNanos(duration.getMean))
      }
      flat = ProcessStats.FlatStats(duration)
      result <- process match {
        case _: Process.ConcurrentProcess =>
          ZIO.effectTotal {
            val subProcessCount = new DescriptiveStatistics()
            spans.foreach { span =>
              subProcessCount.addValue(span.asConcurrent.count)
            }
            ProcessStats.ConcurrentStats(flat = flat, avgSubprocesses = subProcessCount.getMean)
          }
        case _ => ZIO.succeed(flat)
      }
    } yield result

}

object LiveEngine {
  def makeService: ZIO[
    ReportStore with ClusterStore with TraceParser with ProcessStore with Console with Random,
    Nothing,
    LiveEngine
  ] =
    for {
      reportStore <- ZIO.service[ReportStore.Service]
      clusterStore <- ZIO.service[ClusterStore.Service]
      traceParser <- ZIO.service[TraceParser.Service]
      spanStore <- ZIO.service[ProcessStore.Service]
      console <- ZIO.service[Console.Service]
      random <- ZIO.service[Random.Service]
    } yield new LiveEngine(reportStore, clusterStore, traceParser, spanStore, console, random)

  val layer: ZLayer[
    ReportStore with ClusterStore with TraceParser with ProcessStore with Console with Random,
    Nothing,
    Engine
  ] = makeService.toLayer
}
