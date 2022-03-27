package io.github.mcsim4s.dt.model

case class Trace(spans: Map[String, ProcessStats]) {
  def ++(other: Trace) = Trace(spans ++ other.spans)
}

object Trace {
  val empty: Trace = Trace(Map.empty)
}
