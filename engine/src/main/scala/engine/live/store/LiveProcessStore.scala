package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.store.ProcessStore
import io.github.mcsim4s.dt.engine.store.ProcessStore.ProcessStore
import io.github.mcsim4s.dt.model.Process
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio._
import zio.random.Random
import zio.stm._

class LiveProcessStore(processesRef: TMap[ClusterId, Seq[Process]]) extends ProcessStore.Service {

  override def add(clusterId: ClusterId, process: Process): UIO[Unit] =
    STM.atomically {
      for {
        existing <- processesRef.getOrElse(clusterId, Seq.empty)
        _ <- processesRef.put(clusterId, existing :+ process)
      } yield ()
    }
}

object LiveProcessStore {
  def makeService: ZIO[Any, Nothing, LiveProcessStore] =
    for {
      processesRef <- STM.atomically(TMap.make[ClusterId, Seq[Process]]())
    } yield new LiveProcessStore(processesRef)

  val layer: ZLayer[Random, Nothing, ProcessStore] = makeService.toLayer
}
