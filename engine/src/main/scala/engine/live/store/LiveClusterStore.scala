package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.store.ClusterStore
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.model.TraceCluster
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import zio._
import zio.random.Random
import zio.stm._
import zio.stream.ZStream

class LiveClusterStore(clustersRef: TMap[String, Map[String, TraceCluster]]) extends ClusterStore.Service {

  override def getOrCreate(clusterId: ClusterId): UIO[TraceCluster] = {
    STM.atomically(
      for {
        opt <- clustersRef.get(clusterId.reportId)
        res <- opt match {
          case Some(clusters) =>
            clusters.get(clusterId.clusterHash) match {
              case Some(cluster) => STM.succeed(cluster)
              case None =>
                val cluster = TraceCluster(clusterId)
                clustersRef
                  .put(clusterId.reportId, clusters + (clusterId.clusterHash -> cluster))
                  .as(cluster)
            }
          case None =>
            val cluster = TraceCluster(clusterId)
            clustersRef.put(clusterId.reportId, Map(clusterId.reportId -> cluster)).as(cluster)
        }
      } yield res
    )
  }

  override def read(reportId: String): ClusterSource = {
    ZStream
      .fromEffect {
        STM.atomically(clustersRef.getOrElse(reportId, Map.empty)).map(_.values.iterator)
      }
      .flatMap(ZStream.fromIteratorTotal(_))
  }
}

object LiveClusterStore {
  def makeService: ZIO[Any, Nothing, LiveClusterStore] =
    for {
      clustersRef <- STM.atomically(TMap.make[String, Map[String, TraceCluster]]())
    } yield new LiveClusterStore(clustersRef)

  val layer: ZLayer[Random, Nothing, ClusterStore] = makeService.toLayer
}
