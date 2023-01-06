package io.github.mcsim4s.dt.engine.source

import io.github.mcsim4s.dt.model.AnalysisRequest.RawTraceSource
import io.github.mcsim4s.dt.model.DeepTraceError.TraceRetrieveError
import io.github.mcsim4s.dt.model.{DeepTraceError, RawTrace}
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.jaegertracing.api_v2.query.{FindTracesRequest, SpansResponseChunk, TraceQueryParameters}
import zio._
import zio.Clock
import zio.stream.ZStream

class JaegerSource(jaegerClient: QueryServiceClient.Service) {

  def createSource(query: TraceQueryParameters): IO[DeepTraceError, RawTraceSource] =
    ZIO.succeed {
      jaegerClient
        .findTraces(FindTracesRequest(query = Some(query)))
        .mapError(status => TraceRetrieveError(s"Jaeger trace stream failed with $status"))
        .mapConcat(_.spans)
        .groupAdjacentBy(_.traceId)
        .map { case (_, chunk) => RawTrace.apply(chunk.toSeq) }
    }
}

object JaegerSource {

  val makeService: ZIO[QueryServiceClient, Nothing, JaegerSource] = for {
    client <- ZIO.service[QueryServiceClient.Service]
  } yield new JaegerSource(client)

  val layer: ZLayer[QueryServiceClient, Nothing, JaegerSource] = ZLayer(makeService)
}
