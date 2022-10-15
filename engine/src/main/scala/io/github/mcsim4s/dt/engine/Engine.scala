package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError}
import zio.macros.accessible
import zio.IO

@accessible
object Engine {
  type Engine = Service

  trait Service {
    def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport]
  }
}
