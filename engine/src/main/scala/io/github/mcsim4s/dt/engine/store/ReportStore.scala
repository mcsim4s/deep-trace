package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.DeepTraceError.DeepTraceTaskNotFound
import io.github.mcsim4s.dt.model.report.{Report, ReportFilter}
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._
import zio.stream._

import java.util.UUID

trait ReportStore {
  def create(request: AnalysisRequest)(implicit trace: Trace): IO[DeepTraceError, Report]
  def list(filter: ReportFilter = ReportFilter.Any)(implicit trace: Trace): Stream[DeepTraceError, Report]
  def get(id: UUID)(implicit trace: Trace): IO[DeepTraceTaskNotFound, Report]

  def update(
      id: UUID
    )(upd: Report => IO[DeepTraceError, Report]
    )(implicit trace: Trace
    ): IO[DeepTraceError, Report]
}