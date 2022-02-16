package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model.DeepTraceError.TraceRetrieveError
import zio.stream.ZStream

case class AnalysisRequest(
    service: String,
    operation: String,
    traceSource: RawTraceSource
)

object AnalysisRequest {
  type RawTraceSource = ZStream[Any, TraceRetrieveError, RawTrace]
}
