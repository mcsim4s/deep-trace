package io.github.mcsim4s.dt

import TraceCluster.TraceSource

import zio.stream.ZStream

case class TraceCluster(hash: String, traces: TraceSource)

object TraceCluster {
  type ClusterSource = ZStream[Any, Nothing, TraceCluster]
  type TraceSource = ZStream[Any, Nothing, Trace]
}
