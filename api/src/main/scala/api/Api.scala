package io.github.mcsim4s.dt.api

import caliban.schema.GenericSchema
import caliban.{GraphQL, RootResolver}
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.model.TraceCluster
import zio._

object Api extends GenericSchema[ApiService] {
  case class ClusterQueries(
      get: String => URIO[ApiService, TraceCluster]
  )

  val clusters = GraphQL.graphQL(
    RootResolver(
      ClusterQueries(
        get = id => ApiService.getCluster("", id)
      )
    )
  )

  val root: GraphQL[ApiService] = clusters
}
