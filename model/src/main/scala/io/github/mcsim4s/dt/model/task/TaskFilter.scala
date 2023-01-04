package io.github.mcsim4s.dt.model.task

import io.github.mcsim4s.dt.model.query._

import java.util.UUID

case class TaskFilter(taskId: Option[UUID] = None, state: Set[DeepTraceTaskStateFilter] = Set.empty)
