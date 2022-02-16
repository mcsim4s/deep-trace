package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio.stream.ZStream

case class TraceCluster(id: ClusterId, root: Process)

object TraceCluster {
  case class ClusterId(reportId: String, clusterHash: String)

  type ClusterSource = ZStream[Any, DeepTraceError, TraceCluster]
}
