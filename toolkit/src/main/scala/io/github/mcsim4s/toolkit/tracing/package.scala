package io.github.mcsim4s.toolkit

import zio.UIO
import zio.telemetry.opentelemetry.Tracing

package object tracing {

  implicit class RichTracing(val tracing: Tracing) {
    def markFailed: UIO[Unit] = tracing.setAttribute("error", value = true).unit
  }
}
