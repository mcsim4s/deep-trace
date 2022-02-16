package io.github.mcsim4s.dt.api.model

case class TraceQuery(
    serviceName: String,
    operationName: String,
    tags: List[String],
    startTimeMinSeconds: Option[Int],
    startTimeMaxSeconds: Option[Int],
    durationMinMillis: Option[Int],
    durationMaxMillis: Option[Int]
)
