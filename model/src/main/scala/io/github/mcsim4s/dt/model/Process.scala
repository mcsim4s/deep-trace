package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

case class Process(
    service: String,
    operation: String,
    children: Seq[Process]
) {
  lazy val id: ProcessId = {
    val builder = new StringBuilder()
    builder.append(service)
    builder.append(operation)
    children.foreach(child => builder.append(child.id))
    ProcessId(MD5.hash(builder.toString()))
  }
}

object Process {
  case class ProcessId(hash: String)
}
