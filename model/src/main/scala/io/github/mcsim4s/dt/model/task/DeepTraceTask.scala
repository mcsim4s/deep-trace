package io.github.mcsim4s.dt.model.task

import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model.task.DeepTraceTask.State

import java.time.Instant
import java.util.UUID

case class DeepTraceTask(
    id: UUID,
    createdAt: Instant,
    service: String,
    operation: String,
    state: State)

object DeepTraceTask {
  sealed trait State

  case object New extends State
  case class Fetching() extends State
  case object Clustering extends State
  case class ClustersBuilt(clusterIds: Seq[ClusterId]) extends State
}
