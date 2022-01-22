package io.github.mcsim4s.dt.api.model

import scala.concurrent.duration.Duration

case class Process(start: Duration, duration: Duration, children: Seq[Process])
