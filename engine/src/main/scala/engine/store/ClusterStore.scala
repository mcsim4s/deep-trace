package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.TraceCluster
import io.github.mcsim4s.dt.model.TraceCluster._
import zio._
import zio.macros.accessible

@accessible
object ClusterStore {
  type ClusterStore = Has[Service]

  trait Service {
    def getOrCreate(clusterId: ClusterId): UIO[TraceCluster]
    def read(reportId: String): ClusterSource
  }
}
