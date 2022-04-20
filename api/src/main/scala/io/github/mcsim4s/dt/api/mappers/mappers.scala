package io.github.mcsim4s.dt.api

import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, ClusterStats, DeepTraceError, Process, TraceCluster}
import io.github.mcsim4s.dt.model._
import io.github.mcsim4s.dt.api.model.{
  AnalysisReport => ApiReport,
  AnalysisRequest => ApiAnalysisRequest,
  ClusterId => ApiClusterId,
  Process => ApiProcess,
  TraceCluster => ApiCluster
}
import io.github.mcsim4s.dt.model.DeepTraceError.GenericError
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio._

package object mappers {
  def toApi(id: ClusterId): ApiClusterId = ApiClusterId(id.reportId, id.rootHash)

  def toApi(report: AnalysisReport): ApiReport =
    ApiReport(
      id = report.id,
      createdAt = report.createdAt,
      service = report.service,
      operation = report.operation,
      state = toApi(report.state)
    )

  def toApi(state: AnalysisReport.State): ApiReport.State =
    state match {
      case AnalysisReport.Clustering                => ApiReport.Clustering
      case AnalysisReport.ClustersBuilt(clusterIds) => ApiReport.ClustersBuilt(clusterIds.map(toApi))
    }

  def toApi(
      process: Process.ParallelProcess,
      stats: ClusterStats,
      isRoot: Boolean = false
  ): Map[String, ApiProcess] = {
    val current = ApiProcess.ParallelProcess(
      id = process.id.hash,
      isRoot = isRoot,
      service = process.service,
      operation = process.operation,
      childrenIds = process.children.map(_.id.hash),
      stats = stats.processes(process.id.hash).asFlat
    )
    process.children.foldLeft(Map[String, ApiProcess](current.id -> current)) {
      case (acc, child) =>
        acc ++ toApi(child, stats)
    }
  }

  def toApi(
      process: Process.SequentialProcess,
      stats: ClusterStats
  ): Map[String, ApiProcess] = {
    val current = ApiProcess.SequentialProcess(
      id = process.id.hash,
      childrenIds = process.children.map(_.id.hash)
    )
    process.children.foldLeft(Map[String, ApiProcess](current.id -> current)) {
      case (acc, child) =>
        acc ++ toApiProcessGeneral(child, stats)
    }
  }

  def toApi(
      process: Process.ConcurrentProcess,
      stats: ClusterStats
  ): Map[String, ApiProcess] = {
    val current = ApiProcess.ConcurrentProcess(
      id = process.id.hash,
      ofId = process.of.id.hash,
      stats = stats.processes(process.id.hash).asConcurrent
    )
    Map[String, ApiProcess](
      current.id -> current
    ) ++ toApi(process.of, stats)
  }

  def toApi(
      process: Process.Gap,
      stats: ClusterStats
  ): Map[String, ApiProcess] = {
    val current = ApiProcess.Gap(
      id = process.id.hash,
      stats = stats.processes(process.id.hash).asFlat
    )
    Map[String, ApiProcess](
      current.id -> current
    )
  }

  def toApiProcessGeneral(
      process: Process,
      stats: ClusterStats
  ): Map[String, ApiProcess] = {
    process match {
      case par: Process.ParallelProcess          => toApi(par, stats)
      case seq: Process.SequentialProcess        => toApi(seq, stats)
      case concurrent: Process.ConcurrentProcess => toApi(concurrent, stats)
      case gap: Process.Gap                      => toApi(gap, stats)
    }
  }

  def toApi(cluster: TraceCluster): IO[DeepTraceError, ApiCluster] = {
    for {
      avg <-
        ZIO
          .fromOption(cluster.stats)
          .orElseFail(GenericError("Avg Process was empty on cluster request"))
    } yield ApiCluster(
      id = toApi(cluster.id),
      processes = toApi(cluster.root, avg)
    )
  }

  def fromApi(id: ApiClusterId): ClusterId = ClusterId(id.reportId, id.structureHash)
}
