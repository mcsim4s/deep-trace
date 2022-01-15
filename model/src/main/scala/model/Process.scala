package io.github.mcsim4s.dt.model

case class Process(name: String, children: Seq[Process]) {
  lazy val hash: String = "const"
}
