package io.github.mcsim4s.dt.dao

import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.UnexpectedDbError
import io.github.mcsim4s.dt.model.task.{DeepTraceTask, TaskFilter}
import zio._
import zio.stream._

trait TaskDao {
  def create(task: DeepTraceTask)(implicit trace: Trace): IO[UnexpectedDbError, Unit]
  def list(filter: TaskFilter)(implicit trace: Trace): Stream[UnexpectedDbError, DeepTraceTask]
  def updateCas(old: DeepTraceTask, update: DeepTraceTask)(implicit trace: Trace): IO[DeepTraceError, DeepTraceTask]
}
