package io.github.mcsim4s.dt

import io.jaegertracing.api_v2.model.Span

case class RawTrace(spans: Seq[Span])
