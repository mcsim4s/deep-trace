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
      val stream: ZStream[Any, io.grpc.Status, SpansResponseChunk] =
        jaegerClient.findTraces(FindTracesRequest(query = Some(query)))
      stream.mapBoth(
        status => {
          TraceRetrieveError(s"Jaeger trace stream failed with $status")
        },
        chunk => {
          RawTrace(chunk.spans)
        }
      )
    }
}

object JaegerSource {
  val makeService: ZIO[QueryServiceClient, Nothing, JaegerSource] = for {
    client <- ZIO.service[QueryServiceClient.Service]
  } yield new JaegerSource(client)

  val layer: ZLayer[QueryServiceClient, Nothing, JaegerSource] = ZLayer(makeService)
}
