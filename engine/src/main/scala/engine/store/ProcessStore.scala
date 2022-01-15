package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.Process
import zio._
import zio.macros.accessible

@accessible
object ProcessStore {
  type ProcessStore = Has[Service]

  trait Service {
    def add(clusterId: String, process: Process): UIO[Unit]
  }
}
