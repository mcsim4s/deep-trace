package io.github.mcsim4s.dt

case class Process(name: String, children: Seq[Process])

object Process {}
