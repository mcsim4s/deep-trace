package io.github.mcsim4s.dt.api

import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError, Process, Trace, TraceCluster}
import io.github.mcsim4s.dt.model._
import io.github.mcsim4s.dt.api.model.{
  AnalysisReport => ApiReport,
  AnalysisRequest => ApiAnalysisRequest,
  ClusterId => ApiClusterId,
  Process => ApiProcess,
  TraceCluster => ApiCluster
}
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

  def toApi(process: Process, trace: Trace): ApiProcess =
    ApiProcess(
      id = process.id.hash,
      service = process.service,
      operation = process.operation,
      start = trace.spans(process.id.hash).avgStart,
      duration = trace.spans(process.id.hash).avgDuration,
      children = process.children.map(child => toApi(child, trace))
    )

  def toApi(cluster: TraceCluster): IO[DeepTraceError, ApiCluster] = {
    for {
      avg <-
        ZIO
          .fromOption(cluster.avg)
          .orElseFail(new DeepTraceError("Avg Process was empty on cluster request"))
    } yield ApiCluster(
      id = toApi(cluster.id),
      rootProcess = toApi(cluster.root, avg)
    )
  }

  def fromApi(id: ApiClusterId): ClusterId = ClusterId(id.reportId, id.structureHash)
}
