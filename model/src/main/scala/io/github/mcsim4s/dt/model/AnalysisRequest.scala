package io.github.mcsim4s.dt.model

import io.jaegertracing.api_v2.query.TraceQueryParameters

case class AnalysisRequest(
    service: String,
    operation: String,
    query: TraceQueryParameters
)
