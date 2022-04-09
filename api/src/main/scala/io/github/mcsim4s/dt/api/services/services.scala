package io.github.mcsim4s.dt.api

import caliban.wrappers.Wrapper.OverallWrapper
import caliban.{CalibanError, GraphQLRequest, GraphQLResponse}
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError._
import zio._
import zio.clock.Clock
import zio.console.{Console, putStrLn}

package object services {
  implicit class ApiRequest[R, A](val request: ZIO[R, DeepTraceError, A]) {
    lazy val apiRequest: ZIO[R, Nothing, A] = request.flatMapError {
      case err: ReportNotFound  => ZIO.die(new IllegalArgumentException(err.message))
      case err: ClusterNotFound => ZIO.die(new IllegalArgumentException(err.message))
      //      case DeepTraceError.GenericError(message) => ???
      //      case DeepTraceError.RawTraceMappingError(message) => ???
      //      case DeepTraceError.TraceRetrieveError(message) => ???
      //      case DeepTraceError.ExternalGrpcError(service, status) => ???
//      case DeepTraceError.CasConflict(entityType, id) => ???
      case other => ZIO.die(new IllegalStateException(other.message))
    }
  }

  lazy val logging: OverallWrapper[Console with Clock] =
    new OverallWrapper[Console with Clock] {
      def wrap[R1 <: Console with Clock](
          process: GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]]
      ): GraphQLRequest => ZIO[R1, Nothing, GraphQLResponse[CalibanError]] =
        request =>
          process(request).timed
            .tap {
              case (processTime, response) =>
                ZIO.when(response.errors.isEmpty)(
                  putStrLn(
                    s"${request.operationName.getOrElse("EMPTY")} is performed in ${processTime.toMillis}ms"
                  ).orDie
                )
            }
            .map(_._2)
    }
}
