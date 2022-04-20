package io.github.mcsim4s.dt.api.model

import io.github.mcsim4s.dt.model.ProcessStats

sealed trait Process

object Process {
  case class ParallelProcess(
      id: String,
      isRoot: Boolean,
      service: String,
      operation: String,
      childrenIds: Seq[String],
      stats: ProcessStats.FlatStats
  ) extends Process

  case class SequentialProcess(id: String, childrenIds: Seq[String]) extends Process

  case class ConcurrentProcess(
      id: String,
      ofId: String,
      stats: ProcessStats.ConcurrentStats
  ) extends Process

  case class Gap(id: String, stats: ProcessStats.FlatStats) extends Process
}
