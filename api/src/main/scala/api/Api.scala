package io.github.mcsim4s.dt.api

import caliban.schema.GenericSchema
import caliban.{GraphQL, RootResolver}
import io.github.mcsim4s.dt.api.model.TraceCluster
import io.github.mcsim4s.dt.engine.store.ClusterStore
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import zio._

object Api extends GenericSchema[ClusterStore] {
  case class ClusterQueries(
      get: String => URIO[ClusterStore, TraceCluster]
  )

  val clusters = GraphQL.graphQL(
    RootResolver(
      ClusterQueries(
        get = id => ClusterStore.get("", id).map(TraceCluster.fromModel)
      )
    )
  )

  val root = clusters
}
