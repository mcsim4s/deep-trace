package io.github.mcsim4s.dt.api.model

import io.github.mcsim4s.dt.api.model.AnalysisReport.State

import java.time.Instant

case class AnalysisReport(
    id: String,
    createdAt: Instant,
    service: String,
    operation: String,
    state: State)

object AnalysisReport {
  sealed trait State

  case object New extends State

  case object Fetching extends State

  case object Clustering extends State

  case class ClustersBuilt(clusterIds: Seq[ClusterId]) extends State
}
