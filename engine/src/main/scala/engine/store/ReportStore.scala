package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.engine.{AnalysisReport, AnalysisRequest}
import io.github.mcsim4s.dt.model.DeepTraceError
import zio._
import zio.macros.accessible

@accessible
object ReportStore {
  type ReportStore = Has[Service]

  trait Service {
    def create(request: AnalysisRequest): UIO[AnalysisReport]
    def list(): UIO[List[AnalysisReport]]
  }
}
