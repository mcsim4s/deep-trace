package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.DeepTraceError.TraceRetrieveError
import io.github.mcsim4s.dt.model.RawTrace
import io.github.mcsim4s.dt.engine.AnalysisRequest.RawTraceSource
import zio.stream.ZStream

import java.time.Instant

case class AnalysisRequest(
    name: String,
    createTime: Instant,
    traceSource: RawTraceSource
)

object AnalysisRequest {
  type RawTraceSource = ZStream[Any, TraceRetrieveError, RawTrace]
}
