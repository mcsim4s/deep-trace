package io.github.mcsim4s.dt.api

import io.github.mcsim4s.dt.api.model.TraceCluster
import io.github.mcsim4s.dt.engine.store.ClusterStore
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio._
import zio.macros.accessible

@accessible
object ApiService {
  type ApiService = Has[Service]

  trait Service {
    def getCluster(reportId: String, structureHash: String): UIO[TraceCluster]
  }

  class Live(clusterStore: ClusterStore.Service) extends Service {
    override def getCluster(reportId: String, structureHash: String): UIO[TraceCluster] = {
      clusterStore
        .getOrCreate(ClusterId(reportId, structureHash))
        .map(TraceCluster.fromModel)
    }
  }

  val live: ZLayer[ClusterStore, Nothing, ApiService] = ZLayer.fromService[ClusterStore.Service, Service](
    new Live(_)
  )
}
