package io.github.mcsim4s.dt.api

import com.google.protobuf.timestamp.Timestamp
import io.github.mcsim4s.dt.api.mappers._
import io.github.mcsim4s.dt.api.model.{
  AnalysisReport => ApiReport,
  AnalysisRequest => ApiAnalysisRequest,
  ClusterId => ApiClusterId,
  TraceCluster => ApiCluster
}
import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.github.mcsim4s.dt.engine.store.{ClusterStore, ReportStore}
import io.github.mcsim4s.dt.model.DeepTraceError.ReportNotFound
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import io.jaegertracing.api_v2.query.TraceQueryParameters
import zio.macros.accessible
import zio.{IO, ZIO, ZLayer}

@accessible
object ApiService {
  type ApiService = Service

  trait Service {
    def getCluster(id: ApiClusterId): IO[DeepTraceError, ApiCluster]
    def createReport(request: ApiAnalysisRequest): IO[DeepTraceError, ApiReport]
    def listReports(): IO[ReportNotFound, List[ApiReport]]
    def getReport(id: String): IO[DeepTraceError, ApiReport]
  }

  class Live(
      engine: Engine.Service,
      reportStore: ReportStore.Service,
      clusterStore: ClusterStore.Service,
      jaegerSource: JaegerSource
  ) extends Service {
    override def getCluster(id: ApiClusterId): IO[DeepTraceError, ApiCluster] = {
      clusterStore.get(fromApi(id)).flatMap(toApi)
    }

    private def fromMillis(millis: Int): com.google.protobuf.duration.Duration = {
      com.google.protobuf.duration.Duration(
        java.time.Duration.ofMillis(millis)
      )
    }

    override def createReport(request: ApiAnalysisRequest): IO[DeepTraceError, ApiReport] = {
      for {
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
            service = request.params.serviceName,
            operation = request.params.operationName,
            traceSource = source
          )
        )
      } yield toApi(result)
    }

    override def listReports(): IO[ReportNotFound, List[ApiReport]] = reportStore.list().map(_.map(toApi))

    override def getReport(id: String): IO[ReportNotFound, ApiReport] = reportStore.get(id).map(toApi)
  }

  val live: ZLayer[
    Engine.Service with ReportStore.Service with ClusterStore.Service with JaegerSource,
    Nothing,
    ApiService
  ] = {
    ZLayer {
      for {
        engine <- ZIO.service[Engine.Service]
        reportStore <- ZIO.service[ReportStore.Service]
        clusterStore <- ZIO.service[ClusterStore.Service]
        jaegerSource <- ZIO.service[JaegerSource]
      } yield new Live(engine, reportStore, clusterStore, jaegerSource)
    }
  }
}
