package io.github.mcsim4s.dt.api.model

import scala.concurrent.duration.Duration

case class Process(
    id: String,
    service: String,
    operation: String,
    start: Duration,
    duration: Duration,
    parentId: Option[String]
)
