package io.github.mcsim4s.toolkit.app

import zio._
import zio.logging.backend.SLF4J

trait BaseApplication extends zio.ZIOAppDefault {
  type ApplicationLayer

  private val zioLogging: ZLayer[Any, Nothing, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zioLogging

}
