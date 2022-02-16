package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.AnalysisReport.State
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId

import java.time.Instant

case class AnalysisReport(
    id: String,
    createdAt: Instant,
    service: String,
    operation: String,
    state: State
)

object AnalysisReport {
  sealed trait State

  case object Clustering extends State

  case class ClustersBuilt(clusterIds: Seq[ClusterId]) extends State
}
