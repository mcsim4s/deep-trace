package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.report.Report
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._

trait Engine {
  def createReport(request: AnalysisRequest): IO[DeepTraceError, Report]

  def start: ZIO[Any, DeepTraceError, Any]
}

object Engine {
  def createReport(request: AnalysisRequest): ZIO[Engine, DeepTraceError, Report] =
    ZIO.serviceWithZIO[Engine](_.createReport(request))
}
