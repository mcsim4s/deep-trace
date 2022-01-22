package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.Process.ProcessId
import io.jaegertracing.api_v2.model.Span
import zio._
import zio.macros.accessible

@accessible
object SpanStore {
  type SpanStore = Has[Service]

  trait Service {
    def add(processId: ProcessId, span: Span): UIO[Unit]
  }
}
