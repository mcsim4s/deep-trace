package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.task.DeepTraceTask
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._

trait Engine {
  def process(request: AnalysisRequest): IO[DeepTraceError, DeepTraceTask]
}

object Engine {
  def process(request: AnalysisRequest): ZIO[Engine, DeepTraceError, DeepTraceTask] =
    ZIO.serviceWithZIO[Engine](_.process(request))
}
