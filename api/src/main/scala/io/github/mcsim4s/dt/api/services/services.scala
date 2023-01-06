package io.github.mcsim4s.dt.api

import caliban.wrappers.Wrapper.OverallWrapper
import caliban.{CalibanError, GraphQLRequest, GraphQLResponse}
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError._
import io.opentelemetry.api.trace.SpanKind
import zio._
import zio.Clock
import zio.Console
import zio.Console.printLine
import zio.telemetry.opentelemetry.Tracing

package object services {

  implicit class ApiRequest[R, A](val request: ZIO[R, DeepTraceError, A]) {

    lazy val apiRequest: ZIO[R, Nothing, A] = request.flatMapError {
      case err: DeepTraceTaskNotFound => ZIO.die(new IllegalArgumentException(err.message))
      case err: ClusterNotFound => ZIO.die(new IllegalArgumentException(err.message))
      //      case DeepTraceError.GenericError(message) => ???
      //      case DeepTraceError.RawTraceMappingError(message) => ???
      //      case DeepTraceError.TraceRetrieveError(message) => ???
      //      case DeepTraceError.ExternalGrpcError(service, status) => ???
//      case DeepTraceError.CasConflict(entityType, id) => ???
      case other => ZIO.die(new IllegalStateException(other.message))
    }
  }

  lazy val logging: OverallWrapper[Any] =
    new OverallWrapper[Any] {

      def wrap[R1 <: Any](
          process: GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]]): GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]] =
        request =>
          process(request).timed
            .tap { case (processTime, response) =>
              ZIO.when(response.errors.isEmpty)(
                printLine(
                  s"${request.operationName.getOrElse("EMPTY")} is performed in ${processTime.toMillis}ms"
                ).orDie
              )
            }
            .map(_._2)
    }

  lazy val tracing: OverallWrapper[Tracing] =
    new OverallWrapper[Tracing] {

      def wrap[R1 <: Tracing](
          process: GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]]): GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]] =
        request =>
          for {
            tracing <- ZIO.service[Tracing]
            result <- process(request) @@ tracing.aspects.root(
              request.operationName.getOrElse("EMPTY"),
              spanKind = SpanKind.SERVER
            )
          } yield result
    }
}
