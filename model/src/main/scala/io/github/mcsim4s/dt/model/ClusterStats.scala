package io.github.mcsim4s.dt.model

case class ClusterStats(processes: Map[String, ProcessStats]) {
  def ++(other: ClusterStats) = ClusterStats(processes ++ other.processes)
}

object ClusterStats {
  val empty: ClusterStats = ClusterStats(Map.empty)
}
