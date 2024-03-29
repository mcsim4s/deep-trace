package io.github.mcsim4s.toolkit.app

import io.github.mcsim4s.toolkit.tracing.JaegerTracing
import zio._
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.Tracing

trait BaseApplication extends zio.ZIOApp {
  override type Environment = Tracing with Scope
  override val environmentTag: EnvironmentTag[Environment] = EnvironmentTag[Environment]
  type ApplicationEnvironment

  private val zioLogging: ZLayer[Any, Nothing, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Environment] = {
    ZLayer.makeSome[ZIOAppArgs, Environment](
      ZLayer.succeed(Scope.global),
      zioLogging >>> JaegerTracing.live
    )
  }

  def program: ZIO[ApplicationEnvironment, Throwable, Unit]

  def applicationEvn: ZLayer[Environment, Throwable, ApplicationEnvironment]

  override def run: ZIO[Environment with ZIOAppArgs, Any, Any] = {
    program
      .provideLayer(applicationEvn)
      .absorb
  }
}
