package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.engine.TraceParser.TraceParsingState
import io.github.mcsim4s.dt.model.DeepTraceError.ClusterNotFound
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import io.github.mcsim4s.dt.model.{DeepTraceError, TraceCluster}
import zio._

import java.util.UUID

trait ClusterStore {
  def get(id: ClusterId): IO[ClusterNotFound, TraceCluster]
  def getOrCreate(taskId: UUID, root: TraceParsingState): UIO[TraceCluster]
  def list(taskId: UUID): ClusterSource
  def update(id: ClusterId)(upd: TraceCluster => IO[DeepTraceError, TraceCluster]): IO[DeepTraceError, TraceCluster]
}
