package io.github.mcsim4s.dt.api.model

import io.github.mcsim4s.dt.model.{TraceCluster => ModelTraceCluster}

case class TraceCluster()

object TraceCluster {
  def fromModel(cluster: ModelTraceCluster): TraceCluster = {
    TraceCluster()
  }
}
