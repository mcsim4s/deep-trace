package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.DeepTraceError.ClusterNotFound
import io.github.mcsim4s.dt.model.{DeepTraceError, Process, TraceCluster}
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import zio.macros.accessible
import zio._

@accessible
object ClusterStore {
  type ClusterStore = Has[Service]

  trait Service {
    def get(id: ClusterId): IO[ClusterNotFound, TraceCluster]
    def getOrCreate(clusterId: ClusterId): UIO[TraceCluster]
    def list(reportId: String): ClusterSource
    def update(id: ClusterId)(upd: TraceCluster => IO[DeepTraceError, TraceCluster]): IO[DeepTraceError, TraceCluster]
  }
}
