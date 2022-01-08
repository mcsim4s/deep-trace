package io.github.mcsim4s.dt

import zio._
import zio.macros._

@accessible
object Engine {
  type Engine = Has[Service]

  trait Service {
    def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport]
  }
}
