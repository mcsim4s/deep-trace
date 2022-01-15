package io.github.mcsim4s.dt.engine.live.store

import io.github.mcsim4s.dt.engine._
import io.github.mcsim4s.dt.engine.store.ReportStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import zio._
import zio.random.Random
import zio.stm._

class LiveReportStore(reportsRef: TMap[String, AnalysisReport], random: Random.Service) extends ReportStore.Service {

  override def create(request: AnalysisRequest): UIO[AnalysisReport] =
    for {
      id <- random.nextUUID.map(_.toString)
      report = AnalysisReport(
        id = id,
        name = request.name
      )
      _ <- STM.atomically(reportsRef.put(id, report))
    } yield report
}

object LiveReportStore {
  def makeService: ZIO[Random, Nothing, LiveReportStore] =
    for {
      reportsRef <- STM.atomically(TMap.make[String, AnalysisReport]())
      random <- ZIO.service[Random.Service]
    } yield new LiveReportStore(reportsRef, random)

  val layer: ZLayer[Random, Nothing, ReportStore] = makeService.toLayer
}
