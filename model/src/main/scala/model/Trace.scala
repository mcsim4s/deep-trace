package io.github.mcsim4s.dt.model

import zio.stream.ZStream

case class Trace(structureRoot: Process) {
  lazy val hash: String = structureRoot.hash
}

object Trace {

  type TraceSource = ZStream[Any, Nothing, Trace]
}
