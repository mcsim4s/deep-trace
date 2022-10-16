package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.TraceParser
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ProcessStore, ReportStore}
import io.github.mcsim4s.dt.engine.{Engine, TraceParser}
import io.github.mcsim4s.dt.model.Process.ProcessId
import io.github.mcsim4s.dt.model.ProcessStats.DurationStats
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model._
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import zio.{Console, _}

import scala.concurrent.duration.Duration

class LiveEngine(
    reportStore: ReportStore,
    clusterStore: ClusterStore,
    traceParser: TraceParser,
    processStore: ProcessStore
) extends Engine {

  override def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
    for {
      report <- reportStore.create(request)
      _ <- parseTraces(report, request)
        .flatMap { clusters =>
          ZIO.foreachDiscard(clusters)(processCluster) *>
            reportStore.update(report.id) { _ =>
              ZIO.succeed(report.copy(state = AnalysisReport.ClustersBuilt(clusters.map(_.id))))
            }
        }
        .tapBoth(
          err => Console.printLineError(s"Async clustering error. Err: $err").ignore,
          _ => Console.printLine("Async clustering complete").ignore
        )
        .forkDaemon
    } yield report
  }

  private def parseTraces(report: AnalysisReport, request: AnalysisRequest): IO[DeepTraceError, Seq[TraceCluster]] =
    request.traceSource
      .tap(trace => Console.printLine(s"Parsing another trace with ${trace.spans.size} spans").ignore)
      .flatMap(trace => traceParser.parse(trace, request.operation))
      .foreach(root => {
        clusterStore
          .getOrCreate(report.id, root)
          .flatMap { cluster =>
            ZIO.foreach(root.instances)(i => processStore.add(cluster.id, i))
          }
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
        } else ZIO.succeed(Map.empty)
    } yield Map(process.id -> currentStats) ++ children

  def singleProcessStats(clusterId: ClusterId)(process: Process): IO[DeepTraceError, ProcessStats] =
    for {
      spans <- processStore.list(clusterId, process.id)
      duration <- ZIO.succeed {
        val duration = new DescriptiveStatistics()
        spans.foreach { span =>
          duration.addValue(span.duration.toNanos)
        }
        DurationStats(Duration.fromNanos(duration.getMean))
      }
      flat = ProcessStats.FlatStats(duration)
      result <- process match {
        case _: Process.ConcurrentProcess =>
          ZIO.succeed {
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
    ReportStore with ClusterStore with TraceParser with ProcessStore,
    Nothing,
    LiveEngine
  ] =
    for {
      reportStore <- ZIO.service[ReportStore]
      clusterStore <- ZIO.service[ClusterStore]
      traceParser <- ZIO.service[TraceParser]
      spanStore <- ZIO.service[ProcessStore]
    } yield new LiveEngine(reportStore, clusterStore, traceParser, spanStore)

  val layer: ZLayer[
    ReportStore with ClusterStore with TraceParser with ProcessStore,
    Nothing,
    Engine
  ] = ZLayer.fromZIO(makeService)
}
