package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.store.SpanStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.model.Process.ProcessId
import io.jaegertracing.api_v2.model.Span
import zio.random.Random
import zio.stm.{STM, TMap}
import zio.{UIO, ZIO, ZLayer}

class LiveSpanStore(spansRef: TMap[ProcessId, List[Span]]) extends SpanStore.Service {
  override def add(processId: ProcessId, span: Span): UIO[Unit] = {
    STM.atomically {
      spansRef.getOrElse(processId, List.empty).flatMap { old =>
        spansRef.put(processId, old.appended(span))
      }
    }
  }

  override def list(processId: ProcessId): UIO[List[Span]] =
    STM.atomically(spansRef.getOrElse(processId, List.empty))
}

object LiveSpanStore {
  def makeService: ZIO[Any, Nothing, LiveSpanStore] =
    for {
      spansRef <- STM.atomically(TMap.make[ProcessId, List[Span]]())
    } yield new LiveSpanStore(spansRef)

  val layer: ZLayer[Random, Nothing, SpanStore] = makeService.toLayer
}
