package io.github.mcsim4s.dt.model

import scala.concurrent.duration.Duration

sealed trait ProcessStats {
  def asFlat: ProcessStats.FlatStats =
    this match {
      case flat: ProcessStats.FlatStats =>
        flat
      case _ =>
        throw new IllegalStateException(s"Expected flat stats but got ${this.getClass.getName}")
    }

  def asConcurrent: ProcessStats.ConcurrentStats =
    this match {
      case flat: ProcessStats.ConcurrentStats =>
        flat
      case _ =>
        throw new IllegalStateException(s"Expected concurrent stats but got ${this.getClass.getName}")
    }
}

object ProcessStats {
  case class DurationStats(average: Duration)
  case class FlatStats(duration: DurationStats) extends ProcessStats
  case class ConcurrentStats(flat: FlatStats, avgSubprocesses: Double) extends ProcessStats
}
