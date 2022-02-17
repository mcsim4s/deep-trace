package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

import scala.concurrent.duration._

case class Process(id: ProcessId, start: Duration, duration: Duration, name: String, children: Seq[Process]) {
  lazy val hash: String = {
    val builder = new StringBuilder()
    builder.append(name)
    children.foreach(child => builder.append(child.hash))
    MD5.hash(builder.toString())
  }
}

object Process {
  case class ProcessId(id: String)
}
