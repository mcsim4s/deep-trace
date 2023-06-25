package io.github.mcsim4s.dt.model.report

import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model.report.Report.State
import io.jaegertracing.api_v2.query.TraceQueryParameters

import java.time.Instant
import java.util.UUID

case class Report(id: UUID, createdAt: Instant, query: TraceQueryParameters, state: State)

object Report {
  sealed trait State

  case object New extends State
  case class Fetching() extends State
  case object Clustering extends State
  case class ClustersBuilt(clusterIds: Seq[ClusterId]) extends State
}
