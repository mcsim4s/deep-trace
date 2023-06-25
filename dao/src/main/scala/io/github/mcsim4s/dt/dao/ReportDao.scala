package io.github.mcsim4s.dt.dao

import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.UnexpectedDbError
import io.github.mcsim4s.dt.model.report.{Report, ReportFilter}
import zio._
import zio.stream._

trait ReportDao {
  def create(task: Report)(implicit trace: Trace): IO[UnexpectedDbError, Unit]
  def list(filter: ReportFilter)(implicit trace: Trace): Stream[UnexpectedDbError, Report]
  def updateCas(old: Report, update: Report)(implicit trace: Trace): IO[DeepTraceError, Report]
}
