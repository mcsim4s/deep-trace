package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.store.SpanStore

import io.jaegertracing.api_v2.model.Span
import io.opentelemetry.api.trace.SpanId
import zio.UIO
import zio.stm.{STM, TMap}

class LiveSpanStore(spansRef: TMap[String, Span]) extends SpanStore.Service {
  override def add(reportId: String, processId: String, span: Span): UIO[Unit] = {
    val key = s"${reportId}_${processId}_${SpanId.fromBytes(span.spanId.toByteArray)}"
    STM.atomically(spansRef.put(key, span))
  }
}
