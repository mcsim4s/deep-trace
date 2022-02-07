package io.github.mcsim4s.dt.api

import caliban.Value.IntValue.LongNumber
import caliban.schema.{GenericSchema, Schema}
import caliban.{GraphQL, RootResolver}
import caliban.wrappers.Wrappers._
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.model._
import io.github.mcsim4s.dt.engine.AnalysisReport
import zio._
import zio.console.Console

import scala.concurrent.duration._

object Api extends GenericSchema[ApiService] {
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
        getCluster = id => ApiService.getCluster(id)
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

  val root: GraphQL[ApiService with Console] = Seq(
    clusters,
    reports
  ).reduce(_ |+| _).rename(Some("Queries"), Some("Mutations"), Some("Subscriptions")) @@ printErrors
}
