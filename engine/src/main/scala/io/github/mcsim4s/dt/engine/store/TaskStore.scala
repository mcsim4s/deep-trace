package io.github.mcsim4s.dt.engine.store

import io.github.mcsim4s.dt.model.DeepTraceError.DeepTraceTaskNotFound
import io.github.mcsim4s.dt.model.task.DeepTraceTask
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._
import zio.stream._

import java.util.UUID

trait TaskStore {
  def create(request: AnalysisRequest)(implicit trace: Trace): IO[DeepTraceError, DeepTraceTask]
  def list()(implicit trace: Trace): Stream[DeepTraceError, DeepTraceTask]
  def get(id: UUID)(implicit trace: Trace): IO[DeepTraceTaskNotFound, DeepTraceTask]

  def update(
      id: UUID
    )(upd: DeepTraceTask => IO[DeepTraceError, DeepTraceTask]
    )(implicit trace: Trace
    ): IO[DeepTraceError, DeepTraceTask]
}
