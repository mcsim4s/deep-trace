package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError}
import zio._

trait Engine {
  def process(request: AnalysisRequest): IO[DeepTraceError, AnalysisReport]
}

object Engine {
  def process(request: AnalysisRequest): ZIO[Engine, DeepTraceError, AnalysisReport] =
    ZIO.serviceWithZIO[Engine](_.process(request))
}
