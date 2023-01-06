package io.github.mcsim4s.toolkit.app

import io.github.mcsim4s.toolkit.tracing.JaegerTracing
import zio._
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.Tracing

trait BaseApplication extends zio.ZIOApp {
  override type Environment = Tracing
  override val environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]
  type ApplicationLayer

  private val zioLogging: ZLayer[Any, Nothing, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Environment] = zioLogging >>> JaegerTracing.live

}
