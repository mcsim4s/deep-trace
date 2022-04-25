package io.github.mcsim4s.dt.model
import io.github.mcsim4s.dt.model.Process.ProcessId

import scala.concurrent.duration.Duration

sealed trait ProcessInstance {
  def processId: ProcessId
  def start: Duration
  def duration: Duration

  def asConcurrent: ProcessInstance.Concurrent =
    this match {
      case concurrent: ProcessInstance.Concurrent => concurrent
      case single: ProcessInstance.Single =>
        throw new IllegalStateException(s"Expected concurrent instance but got ${this.getClass.getName}")
    }
}

object ProcessInstance {
  case class Single(processId: ProcessId, start: Duration, duration: Duration) extends ProcessInstance
  case class Concurrent(processId: ProcessId, start: Duration, duration: Duration, count: Int) extends ProcessInstance
}
