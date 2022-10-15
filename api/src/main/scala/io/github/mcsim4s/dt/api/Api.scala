package io.github.mcsim4s.dt.api

import caliban.Value.IntValue.LongNumber
import caliban._
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.Wrappers._
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.model.{AnalysisReport, AnalysisRequest, ClusterId, TraceCluster}
import io.github.mcsim4s.dt.api.services._
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService.JaegerService
import zio.Clock
import zio.{RIO, URIO}

import scala.concurrent.duration.Duration
import zio.Console

object Api extends GenericSchema[ApiService with JaegerService] {
  implicit val stateSchema = Schema.gen[Any, AnalysisReport.State]
  implicit lazy val durationSchema: Schema[Any, Duration] =
    scalarSchema("Duration", None, None, duration => LongNumber(duration.toNanos))

  case class ClusterQueries(
      getCluster: ClusterId => URIO[ApiService, TraceCluster]
  )

  case class ReportQueries(
      listReports: RIO[ApiService, List[AnalysisReport]],
      getReport: String => RIO[ApiService, AnalysisReport]
  )

  case class ReportsMutations(
      createReport: AnalysisRequest => RIO[ApiService, AnalysisReport]
  )

  val clusters: GraphQL[ApiService with JaegerService] = GraphQL.graphQL(
    RootResolver(
      ClusterQueries(
        getCluster = id => ApiService.getCluster(id).orDieWith(err => new IllegalStateException(err.message))
      )
    )
  )

  val reports: GraphQL[ApiService with JaegerService] = GraphQL.graphQL(
    RootResolver(
      ReportQueries(
        listReports = ApiService.listReports().orDieWith(err => new IllegalStateException(err.message)),
        getReport = (id) => ApiService.getReport(id).orDieWith(err => new IllegalStateException(err.message))
      ),
      ReportsMutations(
        createReport =
          request => ApiService.createReport(request).orDieWith(err => new IllegalStateException(err.message))
      )
    )
  )

  val jaegerService: GraphQL[ApiService with JaegerService] = GraphQL.graphQL(RootResolver(JaegerService.queries))

  val root: GraphQL[ApiService with JaegerService] = Seq(
    clusters,
    reports,
    jaegerService
  ).reduce(_ |+| _).rename(Some("Queries"), Some("Mutations"), Some("Subscriptions")) @@ printErrors @@ logging
}
