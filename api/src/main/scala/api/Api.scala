package io.github.mcsim4s.dt.api

import caliban.Value.IntValue.LongNumber
import caliban.schema.{GenericSchema, Schema}
import caliban.{GraphQL, RootResolver}
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.model._
import zio._
import scala.concurrent.duration._

object Api extends GenericSchema[ApiService] {

  implicit lazy val durationSchema: Schema[Any, Duration] =
    scalarSchema("Duration", None, None, duration => LongNumber(duration.toNanos))

  case class ClusterQueries(
      getCluster: ClusterId => URIO[ApiService, TraceCluster]
  )

  val clusters = GraphQL.graphQL(
    RootResolver(
      ClusterQueries(
        getCluster = id => ApiService.getCluster(id)
      )
    )
  )

  val root: GraphQL[ApiService] = clusters
}
