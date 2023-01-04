package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine.TraceParser.TraceParsingState
import io.github.mcsim4s.dt.engine.store.ClusterStore
import io.github.mcsim4s.dt.model.DeepTraceError.{CasConflict, ClusterNotFound}
import io.github.mcsim4s.dt.model.TraceCluster.{ClusterId, ClusterSource}
import io.github.mcsim4s.dt.model.{DeepTraceError, TraceCluster}
import zio._
import zio.stm._
import zio.stream.ZStream

import java.util.UUID

class LiveClusterStore(clustersRef: TMap[UUID, Map[String, TraceCluster]]) extends ClusterStore {

  override def get(id: ClusterId): IO[DeepTraceError.ClusterNotFound, TraceCluster] =
    STM.atomically(getStm(id))

  private def getStm(id: ClusterId): ZSTM[Any, ClusterNotFound, TraceCluster] =
    clustersRef
      .get(id.taskId)
      .flatMap(opt => ZSTM.fromOption(opt))
      .orElseFail(ClusterNotFound(id))
      .map(_.get(id.rootHash))
      .flatMap(opt => ZSTM.fromOption(opt))
      .orElseFail(ClusterNotFound(id))

  private def fromParsingResult(taskId: UUID, parsingResult: TraceParsingState): TraceCluster =
    TraceCluster(
      ClusterId(taskId, parsingResult.process.id.hash),
      parsingResult.process,
      containsErrors = parsingResult.containsErrors,
      stats = None,
      exampleTraceId = parsingResult.exampleId
    )

  override def getOrCreate(taskId: UUID, root: TraceParsingState): UIO[TraceCluster] = {
    val clusterId = ClusterId(taskId, root.process.id.hash)
    STM.atomically(
      for {
        opt <- clustersRef.get(clusterId.taskId)
        res <- opt match {
          case Some(clusters) =>
            clusters.get(clusterId.rootHash) match {
              case Some(cluster) => STM.succeed(cluster)
              case None =>
                val cluster = fromParsingResult(taskId, root)
                clustersRef
                  .put(clusterId.taskId, clusters + (clusterId.rootHash -> cluster))
                  .as(cluster)
            }
          case None =>
            val cluster = fromParsingResult(taskId, root)
            clustersRef.put(clusterId.taskId, Map(clusterId.rootHash -> cluster)).as(cluster)
        }
      } yield res
    )
  }

  override def list(taskId: UUID): ClusterSource = {
    ZStream
      .fromZIO {
        STM.atomically(clustersRef.getOrElse(taskId, Map.empty)).map(_.values.iterator)
      }
      .flatMap(ZStream.fromIteratorSucceed(_))
  }

  override def update(
      id: ClusterId
    )(upd: TraceCluster => IO[DeepTraceError, TraceCluster]): IO[DeepTraceError, TraceCluster] =
    (for {
      old <- get(id)
      result <- upd(old)
      _ <- updateCas(old, result)
    } yield result).retry(CasRetryPolicy)

  private def updateCas(from: TraceCluster, to: TraceCluster): IO[DeepTraceError, TraceCluster] =
    STM.atomically {
      for {
        old <- getStm(from.id)
        _ <- STM.fail(CasConflict("Trace cluster", from.id.toString)).when(old != from)
        oldMap <- clustersRef.get(from.id.taskId)
        _ <- STM.dieMessage("Report map is empty in update").when(oldMap.isEmpty)
        _ <- clustersRef.put(from.id.taskId, oldMap.get + (to.id.rootHash -> to))
      } yield to
    }
}

object LiveClusterStore {

  def makeService: ZIO[Any, Nothing, LiveClusterStore] =
    for {
      clustersRef <- STM.atomically(TMap.make[UUID, Map[String, TraceCluster]]())
    } yield new LiveClusterStore(clustersRef)

  val layer: ZLayer[Any, Nothing, ClusterStore] = ZLayer(makeService)
}
