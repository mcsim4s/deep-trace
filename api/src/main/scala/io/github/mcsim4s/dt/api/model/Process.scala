package io.github.mcsim4s.dt.api.model

import io.github.mcsim4s.dt.model.ProcessStats

case class Process(
    id: String,
    service: String,
    operation: String,
    parentId: Option[String],
    stats: ProcessStats
)
