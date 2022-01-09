package io.github.mcsim4s.dt
package store

import zio._
import zio.macros.accessible

@accessible
object ProcessStore {
  type ProcessStore = Has[Service]

  trait Service {
    def add(clusterId: String, process: Process): UIO[Unit]
  }
}
