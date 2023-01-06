package io.github.mcsim4s.dt.api.services.jaeger

import caliban.schema.Schema
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.ExternalGrpcError
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.jaegertracing.api_v2.query.{GetOperationsRequest, GetServicesRequest}
import zio._

object JaegerService {
  type JaegerService = Service

  trait Service {
    def suggest(request: SuggestRequest): IO[DeepTraceError, SuggestResponse]
  }

  case class Live(jaegerClient: QueryServiceClient.Service) extends Service {

    def suggest(request: SuggestRequest): IO[DeepTraceError, SuggestResponse] =
      for {
        services <-
          jaegerClient
            .getServices(GetServicesRequest())
            .mapBoth(s => ExternalGrpcError("jaeger", s), _.services)
        operations <- request.serviceName.orElse(services.headOption) match {
          case Some(value) =>
            jaegerClient
              .getOperations(GetOperationsRequest(service = value))
              .mapBoth(s => ExternalGrpcError("jaeger", s), _.operations)
          case None => ZIO.succeed(Seq.empty)
        }

      } yield SuggestResponse(
        services = services,
        operations = operations.map(OperationSuggest.apply),
        forService = request.serviceName
      )
  }

  val layer: ZLayer[QueryServiceClient, Nothing, JaegerService] = ZLayer {
    for {
      queryService <- ZIO.service[QueryServiceClient.Service]
    } yield Live(queryService)
  }

  case class Queries(
      suggest: SuggestRequest => URIO[JaegerService, SuggestResponse])

  implicit val suggestReqSchema: Schema[Any, SuggestRequest] = Schema.genMacro[SuggestRequest].schema
  implicit val suggestRespSchema: Schema[Any, SuggestResponse] = Schema.genMacro[SuggestResponse].schema

  val queries: Queries =
    Queries(
      suggest = request => ZIO.environmentWithZIO[JaegerService](_.get.suggest(request)).apiRequest
    )
}
