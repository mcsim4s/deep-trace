package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.DeepTraceError.ReportNotFound
import io.github.mcsim4s.dt.model.{AnalysisReport, AnalysisRequest, DeepTraceError}
import zio.macros.accessible
import zio._

trait ReportStore {
  def create(request: AnalysisRequest): UIO[AnalysisReport]
  def list(): UIO[List[AnalysisReport]]
  def get(id: String): IO[ReportNotFound, AnalysisReport]
  def update(reportId: String)(
      upd: AnalysisReport => IO[DeepTraceError, AnalysisReport]
  ): IO[DeepTraceError, AnalysisReport]
}
