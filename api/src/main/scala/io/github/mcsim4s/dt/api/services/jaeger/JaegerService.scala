package io.github.mcsim4s.dt.api.services.jaeger

import caliban.schema.Schema
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.ExternalGrpcError
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.jaegertracing.api_v2.query.{GetOperationsRequest, GetServicesRequest}
import zio._

object JaegerService {
  type JaegerService = Has[Service]

  trait Service {
    def suggest(request: SuggestRequest): IO[DeepTraceError, SuggestResponse]
  }

  case class Live(jaegerClient: QueryServiceClient.Service) extends Service {
    def suggest(request: SuggestRequest): IO[DeepTraceError, SuggestResponse] =
      for {
        services <-
          jaegerClient
            .getServices(GetServicesRequest())
            .mapError(s => ExternalGrpcError("jaeger", s))
        operations <- request.serviceName match {
          case Some(value) =>
            jaegerClient
              .getOperations(GetOperationsRequest(service = value))
              .mapBoth(s => ExternalGrpcError("jaeger", s), _.operations)
          case None => ZIO.succeed(Seq.empty)
        }

      } yield SuggestResponse(
        services = services.services,
        operations = operations.map(OperationSuggest.apply)
      )
  }

  val layer: ZLayer[QueryServiceClient, Nothing, JaegerService] =
    ZLayer.fromService[QueryServiceClient.Service, Service](Live.apply)

  case class Queries(
      suggest: SuggestRequest => URIO[JaegerService, SuggestResponse]
  )

  implicit val suggestReqSchema: Schema[Any, SuggestRequest] = Schema.genMacro[SuggestRequest].schema
  implicit val suggestRespSchema: Schema[Any, SuggestResponse] = Schema.genMacro[SuggestResponse].schema

  val queries: Queries =
    Queries(
      suggest = request => ZIO.accessM[JaegerService](_.get.suggest(request)).apiRequest
    )
}
