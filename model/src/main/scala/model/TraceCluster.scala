package io.github.mcsim4s.dt.model

import zio.stream.ZStream

case class TraceCluster(id: String, name: String)

object TraceCluster {
  type ClusterSource = ZStream[Any, DeepTraceError, TraceCluster]
}
