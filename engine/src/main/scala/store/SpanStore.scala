package io.github.mcsim4s.dt
package store

import io.jaegertracing.api_v2.model.Span
import zio._
import zio.macros.accessible

@accessible
object SpanStore {
  type SpanStore = Has[Service]

  trait Service {
    def add(reportId: String, processId: String, span: Span): UIO[Unit]
  }
}
