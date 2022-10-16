package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.Process.ProcessId
import io.github.mcsim4s.dt.model.ProcessInstance
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio.UIO

trait ProcessStore {
  def add(clusterId: ClusterId, instance: ProcessInstance): UIO[Unit]
  def list(clusterId: ClusterId, processId: ProcessId): UIO[List[ProcessInstance]]
}
