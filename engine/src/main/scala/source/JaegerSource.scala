package io.github.mcsim4s.dt
package source

import DeepTraceError.TraceRetrieveError

import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.jaegertracing.api_v2.query.{FindTracesRequest, SpansResponseChunk, TraceQueryParameters}
import zio._
import zio.clock.Clock
import zio.stream.ZStream

case class JaegerSource(jaegerClient: QueryServiceClient.Service, clock: Clock.Service) {
  def createTask(query: TraceQueryParameters): Task[AnalysisRequest] = {
    val stream: ZStream[Any, io.grpc.Status, SpansResponseChunk] =
      jaegerClient.findTraces(FindTracesRequest(query = Some(query)))
    clock.instant.map { now =>
      AnalysisRequest(
        name = "",
        createTime = now,
        traceSource = stream
          .map(chunk => RawTrace(chunk.spans))
          .mapError(status => TraceRetrieveError(s"Jaeger trace stream failed with ${status}"))
      )
    }
  }
}
