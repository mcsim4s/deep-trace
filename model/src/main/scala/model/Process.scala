package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

case class Process(name: String, children: Seq[Process]) {
  lazy val id: ProcessId = ProcessId("const")
}

object Process {
  case class ProcessId(hash: String)
}
