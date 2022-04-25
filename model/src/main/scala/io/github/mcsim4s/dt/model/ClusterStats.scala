package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

case class ClusterStats(traceCount: Int, containsErrors: Boolean, processes: Map[ProcessId, ProcessStats]) {
  def ++(other: ClusterStats): ClusterStats =
    this.copy(
      processes = processes ++ other.processes
    )
}

object ClusterStats {
  val empty: ClusterStats = ClusterStats(traceCount = 0, containsErrors = false, Map.empty)
}
