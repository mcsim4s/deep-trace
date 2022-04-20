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
  case class FlatStats(avgStart: Duration, avgDuration: Duration, allDurations: Seq[Duration]) extends ProcessStats
  case class ConcurrentStats(flat: FlatStats, avgSubprocesses: Int) extends ProcessStats
}
