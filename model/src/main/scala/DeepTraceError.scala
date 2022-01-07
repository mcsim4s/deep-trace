package io.github.mcsim4s.dt

sealed abstract class DeepTraceError(msg: String)

object DeepTraceError {
  case class RawTraceMappingError(msg: String) extends DeepTraceError(msg)
}
