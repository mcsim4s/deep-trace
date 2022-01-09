package io.github.mcsim4s.dt
package store

import io.github.mcsim4s.dt.TraceCluster.ClusterSource
import zio._
import zio.macros.accessible
import zio.stream.ZStream

@accessible
object ClusterStore {
  type ClusterStore = Has[Service]

  trait Service {
    def getOrCreate(reportId: String, structureHash: String): UIO[TraceCluster]
    def read(reportId: String): ClusterSource
  }
}
