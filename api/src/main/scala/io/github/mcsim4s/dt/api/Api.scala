package io.github.mcsim4s.dt.api

import caliban.Value.IntValue.LongNumber
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.Wrapper._
import caliban.wrappers.Wrappers._
import caliban._
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.model.{AnalysisRequest, ClusterId, TraceCluster, AnalysisReport}
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.{RIO, URIO, ZIO}

import scala.concurrent.duration.Duration

object Api extends GenericSchema[ApiService] {
  implicit val stateSchema = Schema.gen[Any, AnalysisReport.State]
  implicit lazy val durationSchema: Schema[Any, Duration] =
    scalarSchema("Duration", None, None, duration => LongNumber(duration.toNanos))

  case class ClusterQueries(
      getCluster: ClusterId => URIO[ApiService, TraceCluster]
  )

  case class ReportQueries(
      listReports: RIO[ApiService, List[AnalysisReport]]
  )

  case class ReportsMutations(
      createReport: AnalysisRequest => RIO[ApiService, AnalysisReport]
  )

  val clusters: GraphQL[ApiService] = GraphQL.graphQL(
    RootResolver(
      ClusterQueries(
        getCluster = id => ApiService.getCluster(id).orDieWith(err => new IllegalStateException(err.message))
      )
    )
  )

  val reports: GraphQL[ApiService] = GraphQL.graphQL(
    RootResolver(
      ReportQueries(
        listReports = ApiService.listReports().orDieWith(err => new IllegalStateException(err.message))
      ),
      ReportsMutations(
        createReport =
          request => ApiService.createReport(request).orDieWith(err => new IllegalStateException(err.message))
      )
    )
  )

  val root: GraphQL[ApiService with Console with Clock] = Seq(
    clusters,
    reports
  ).reduce(_ |+| _).rename(Some("Queries"), Some("Mutations"), Some("Subscriptions")) @@ printErrors @@ logging

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
                  putStrLn(s"${request.operationName} is performed in ${processTime.toMillis}ms").orDie
                )
            }
            .map(_._2)
    }
}