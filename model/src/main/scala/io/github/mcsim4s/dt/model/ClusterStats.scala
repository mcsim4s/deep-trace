package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

case class ClusterStats(
    processes: Map[ProcessId, ProcessStats],
    traceCount: Int
) {
  def ++(other: ClusterStats): ClusterStats =
    this.copy(
      processes = processes ++ other.processes
    )
}

object ClusterStats {
  val empty: ClusterStats = ClusterStats(traceCount = 0, processes = Map.empty)
}
