package io.github.mcsim4s.dt.model

sealed abstract class DeepTraceError(msg: String)

object DeepTraceError {
  case class RawTraceMappingError(msg: String) extends DeepTraceError(msg)

  case class TraceRetrieveError(msg: String) extends DeepTraceError(msg)
}
