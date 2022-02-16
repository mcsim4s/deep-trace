package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.DeepTraceError.ClusterNotFound
import io.github.mcsim4s.dt.model.{Process, TraceCluster}
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import zio.macros.accessible
import zio._

@accessible
object ClusterStore {
  type ClusterStore = Has[Service]

  trait Service {
    def get(id: ClusterId): IO[ClusterNotFound, TraceCluster]
    def getOrCreate(reportId: String, process: => Process): UIO[TraceCluster]
    def read(reportId: String): ClusterSource
  }
}
