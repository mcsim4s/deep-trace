package io.github.mcsim4s.dt.api

import com.google.protobuf.timestamp.Timestamp
import io.github.mcsim4s.dt.api.model.{
  AnalysisRequest => ApiAnalysisRequest,
  ClusterId => ApiClusterId,
  Process => ApiProcess,
  TraceCluster => ApiCluster
}
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.{AnalysisReport, AnalysisRequest, Engine, source}
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ReportStore}
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.github.mcsim4s.dt.engine.store.ReportStore.ReportStore
import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.jaegertracing.api_v2.query.TraceQueryParameters
import zio._
import zio.clock.Clock
import zio.macros.accessible

import scala.concurrent.duration._

@accessible
object ApiService {
  type ApiService = Has[Service]

  trait Service {
    def getCluster(id: ApiClusterId): UIO[ApiCluster]
    def createReport(request: ApiAnalysisRequest): IO[DeepTraceError, AnalysisReport]
    def listReports(): IO[DeepTraceError, List[AnalysisReport]]
  }

  class Live(
      engine: Engine.Service,
      reportStore: ReportStore.Service,
      clusterStore: ClusterStore.Service,
      jaegerSource: JaegerSource,
      clock: Clock.Service
  ) extends Service {
    override def getCluster(id: ApiClusterId): UIO[ApiCluster] = {
      clusterStore
        .getOrCreate(ClusterId(id.reportId, id.structureHash))
        .as(
          ApiCluster(
            id,
            ApiProcess(
              0.nanos,
              150.millis,
              Seq(
                ApiProcess(20.millis, 40.millis, Seq.empty),
                ApiProcess(80.millis, 20.millis, Seq.empty)
              )
            )
          )
        )
    }

    private def fromMillis(millis: Int): com.google.protobuf.duration.Duration = {
      com.google.protobuf.duration.Duration(
        java.time.Duration.ofMillis(millis)
      )
    }

    override def createReport(request: ApiAnalysisRequest): IO[DeepTraceError, AnalysisReport] = {
      for {
        now <- clock.instant
        source <- jaegerSource.createSource(
          TraceQueryParameters(
            serviceName = request.params.serviceName,
            operationName = request.params.operationName,
            tags = request.params.tags.map(pair => pair.split(",").head -> pair.split(",").last).toMap,
            startTimeMax = request.params.startTimeMaxSeconds.map(sec => Timestamp.of(sec, 0)),
            startTimeMin = request.params.startTimeMinSeconds.map(sec => Timestamp.of(sec, 0)),
            durationMax = request.params.durationMaxMillis.map(fromMillis),
            durationMin = request.params.durationMinMillis.map(fromMillis)
          )
        )
        result <- engine.process(
          AnalysisRequest(
            name = s"${request.params.serviceName} - ${request.params.operationName}",
            createTime = now,
            traceSource = source
          )
        )
      } yield result
    }

    override def listReports(): IO[DeepTraceError, List[AnalysisReport]] = reportStore.list()
  }

  val live: ZLayer[Engine with ReportStore with ClusterStore with Has[JaegerSource] with Clock, Nothing, ApiService] =
    ZLayer
      .fromServices[Engine.Service, ReportStore.Service, ClusterStore.Service, JaegerSource, Clock.Service, Service](
        new Live(_, _, _, _, _)
      )
}
