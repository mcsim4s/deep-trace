package io.github.mcsim4s.dt.engine.source

import io.github.mcsim4s.dt.engine.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model.DeepTraceError.TraceRetrieveError
import io.github.mcsim4s.dt.model.{DeepTraceError, RawTrace}
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.jaegertracing.api_v2.query.{FindTracesRequest, SpansResponseChunk, TraceQueryParameters}
import zio._
import zio.clock.Clock
import zio.stream.ZStream

class JaegerSource(jaegerClient: QueryServiceClient.Service, clock: Clock.Service) {
  def createSource(query: TraceQueryParameters): IO[DeepTraceError, RawTraceSource] =
    ZIO.effectTotal {
      val stream: ZStream[Any, io.grpc.Status, SpansResponseChunk] =
        jaegerClient.findTraces(FindTracesRequest(query = Some(query)))
      stream.mapBoth(
        status => TraceRetrieveError(s"Jaeger trace stream failed with ${status}"),
        chunk => RawTrace(chunk.spans)
      )
    }
}

object JaegerSource {
  val makeService: ZIO[Clock with QueryServiceClient, Nothing, JaegerSource] = for {
    client <- ZIO.service[QueryServiceClient.Service]
    clock <- ZIO.service[Clock.Service]
  } yield new JaegerSource(client, clock)

  val layer: ZLayer[Clock with QueryServiceClient, Nothing, Has[JaegerSource]] = makeService.toLayer
}
