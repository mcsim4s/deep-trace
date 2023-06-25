package io.github.mcsim4s.dt.model.report

import io.github.mcsim4s.dt.model.query._

import java.util.UUID

case class ReportFilter(taskId: Option[UUID] = None, state: Set[ReportStateName] = Set.empty)

object ReportFilter {
  val Any: ReportFilter = ReportFilter(
    taskId = None,
    state = Set.empty
  )
}
