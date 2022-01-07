package io.github.mcsim4s.dt

import AnalysisRequest.RawTraceSource
import zio.stream.ZStream

import java.time.Instant

case class AnalysisRequest(
    name: String,
    createTime: Instant,
    traceSource: RawTraceSource
)

object AnalysisRequest {
  type RawTraceSource = ZStream[Any, Nothing, RawTrace]
}
