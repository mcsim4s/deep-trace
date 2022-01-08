package io.github.mcsim4s.dt

import zio._

object Main extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program =
      for {
        request <- ZIO.service[JaegerSource]()
        report <- Engine.process(request)
      } yield report
  }
}
