package io.github.mcsim4s.dt.model

case class ClusterStats(spans: Map[String, ProcessStats]) {
  def ++(other: ClusterStats) = ClusterStats(spans ++ other.spans)
}

object ClusterStats {
  val empty: ClusterStats = ClusterStats(Map.empty)
}
