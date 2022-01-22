package io.github.mcsim4s.dt.api

import io.github.mcsim4s.dt.api.model.{ClusterId => ApiClusterId, Process => ApiProcess, TraceCluster => ApiCluster}
import io.github.mcsim4s.dt.engine.store.ClusterStore
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import zio._
import zio.clock.Clock
import zio.macros.accessible

import scala.concurrent.duration._

@accessible
object ApiService {
  type ApiService = Has[Service]

  trait Service {
    def getCluster(id: ApiClusterId): UIO[ApiCluster]
  }

  class Live(clusterStore: ClusterStore.Service, clock: Clock.Service) extends Service {
    override def getCluster(id: ApiClusterId): UIO[ApiCluster] = {
      clock.sleep(zio.duration.Duration.fromScala(1.second)) *>
        clusterStore
          .getOrCreate(ClusterId(id.reportId, id.structureHash))
          .as(ApiCluster(id, ApiProcess(0.nanos, 150.millis, Seq.empty)))
    }
  }

  val live: ZLayer[ClusterStore with Clock, Nothing, ApiService] =
    ZLayer.fromServices[ClusterStore.Service, Clock.Service, Service](
      new Live(_, _)
    )
}
