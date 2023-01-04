package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.dao.TaskDao
import io.github.mcsim4s.dt.engine.store.TaskStore
import io.github.mcsim4s.dt.model.DeepTraceError.DeepTraceTaskNotFound
import io.github.mcsim4s.dt.model.task._
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import zio._
import zio.stream._

import java.util.UUID

class LiveTaskStore(taskDao: TaskDao) extends TaskStore {

  override def create(request: AnalysisRequest)(implicit trace: Trace): IO[DeepTraceError, DeepTraceTask] =
    for {
      id <- Random.nextUUID
      now <- Clock.instant
      task = DeepTraceTask(
        id = id,
        createdAt = now,
        service = request.service,
        operation = request.operation,
        state = DeepTraceTask.New
      )
      _ <- taskDao.create(task)
    } yield task

  override def list()(implicit trace: Trace): Stream[DeepTraceError, DeepTraceTask] =
    taskDao.list(filter = TaskFilter())

  override def update(
      id: UUID
    )(upd: DeepTraceTask => IO[DeepTraceError, DeepTraceTask]
    )(implicit trace: Trace): IO[DeepTraceError, DeepTraceTask] =
    (for {
      old <- get(id)
      result <- upd(old)
      _ <- taskDao.updateCas(old, result)
    } yield result).retry(CasRetryPolicy)

  def get(id: UUID)(implicit trace: Trace): IO[DeepTraceTaskNotFound, DeepTraceTask] = {
    taskDao
      .list(filter = TaskFilter(taskId = Some(id)))
      .runHead
      .flatMap(r => ZIO.fromOption(r))
      .orElseFail(DeepTraceTaskNotFound(id))
  }
}

object LiveTaskStore {

  val layer: ZLayer[TaskDao, Nothing, TaskStore] = ZLayer {
    for {
      dao <- ZIO.service[TaskDao]
    } yield new LiveTaskStore(dao)
  }
}
