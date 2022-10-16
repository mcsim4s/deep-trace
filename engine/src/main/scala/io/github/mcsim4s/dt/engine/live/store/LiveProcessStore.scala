package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.live.store.LiveProcessStore.InstanceId
import io.github.mcsim4s.dt.engine.store.ProcessStore
import io.github.mcsim4s.dt.model.Process.ProcessId
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.github.mcsim4s.dt.model.{ProcessInstance, TraceCluster}
import zio.stm.{STM, TMap}
import zio.{UIO, ZIO, ZLayer}

class LiveProcessStore(spansRef: TMap[InstanceId, List[ProcessInstance]]) extends ProcessStore {

  override def add(clusterId: TraceCluster.ClusterId, instance: ProcessInstance): UIO[Unit] =
    STM.atomically {
      val id = clusterId -> instance.processId
      spansRef.getOrElse(id, List.empty).flatMap { old =>
        spansRef.put(id, old.appended(instance))
      }
    }

  override def list(clusterId: TraceCluster.ClusterId, processId: ProcessId): UIO[List[ProcessInstance]] =
    STM.atomically(spansRef.getOrElse(clusterId -> processId, List.empty))
}

object LiveProcessStore {
  type InstanceId = (ClusterId, ProcessId)

  def makeService: ZIO[Any, Nothing, LiveProcessStore] =
    for {
      spansRef <- STM.atomically(TMap.make[InstanceId, List[ProcessInstance]]())
    } yield new LiveProcessStore(spansRef)

  val layer: ZLayer[Any, Nothing, ProcessStore] = ZLayer(makeService)
}
