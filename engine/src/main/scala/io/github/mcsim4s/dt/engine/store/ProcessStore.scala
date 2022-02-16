package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.Process
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio.macros.accessible
import zio.{Has, UIO}

@accessible
object ProcessStore {
  type ProcessStore = Has[Service]

  trait Service {
    def add(clusterId: ClusterId, process: Process): UIO[Unit]
  }
}
