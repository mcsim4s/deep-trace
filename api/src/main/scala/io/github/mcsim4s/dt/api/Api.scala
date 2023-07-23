package io.github.mcsim4s.dt.api

import caliban.Value.IntValue.LongNumber
import caliban._
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.Wrappers._
import io.github.mcsim4s.dt.api.model.{AnalysisReport, AnalysisRequest, ClusterId, TraceCluster}
import io.github.mcsim4s.dt.api.services._
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService.JaegerService
import zio.RIO
import zio.telemetry.opentelemetry.Tracing

import scala.concurrent.duration.Duration

object Api extends GenericSchema[ApiService with JaegerService] {
  type Environment = ApiService with JaegerService with Tracing

  implicit val durationSchema: Schema[Any, Duration] =
    scalarSchema("Duration", None, None, duration => LongNumber(duration.toNanos))

  case class ClusterQueries(getCluster: ClusterId => RIO[ApiService, TraceCluster])

  case class ReportQueries(
      listReports: RIO[ApiService, List[AnalysisReport]],
      getReport: String => RIO[ApiService, AnalysisReport]
  )

  case class ReportsMutations(createReport: AnalysisRequest => RIO[ApiService, AnalysisReport])

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
        getReport = id => ApiService.getReport(id).orDieWith(err => new IllegalStateException(err.message))
      ),
      ReportsMutations(
        createReport =
          request => ApiService.createReport(request).orDieWith(err => new IllegalStateException(err.message))
      )
    )
  )

  val jaegerService: GraphQL[ApiService with JaegerService] = GraphQL.graphQL(RootResolver(JaegerService.queries))

  val root: GraphQL[Environment] = Seq(
    clusters,
    reports,
    jaegerService
  ).reduce(_ |+| _)
    .rename(Some("Queries"), Some("Mutations"), Some("Subscriptions")) @@ printErrors @@ logging @@ tracing
}
