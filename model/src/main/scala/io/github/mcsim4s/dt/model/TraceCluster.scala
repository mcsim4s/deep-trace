package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ParallelProcess
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio.stream.ZStream

case class TraceCluster(
    id: ClusterId,
    root: ParallelProcess,
    containsErrors: Boolean,
    exampleTraceId: String,
    stats: Option[ClusterStats]
)

object TraceCluster {
  case class ClusterId(reportId: String, rootHash: String)

  type ClusterSource = ZStream[Any, DeepTraceError, TraceCluster]
}
