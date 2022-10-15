package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.Process.ProcessId
import io.github.mcsim4s.dt.model.ProcessInstance
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio.macros.accessible
import zio.UIO

@accessible
object ProcessStore {
  type ProcessStore = Service

  trait Service {
    def add(clusterId: ClusterId, instance: ProcessInstance): UIO[Unit]
    def list(clusterId: ClusterId, processId: ProcessId): UIO[List[ProcessInstance]]
  }
}
