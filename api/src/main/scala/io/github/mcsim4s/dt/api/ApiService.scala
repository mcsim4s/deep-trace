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
import io.github.mcsim4s.dt.engine.store.{ClusterStore, TaskStore}
import io.github.mcsim4s.dt.model.DeepTraceError.DeepTraceTaskNotFound
import io.github.mcsim4s.dt.model.{AnalysisRequest, DeepTraceError}
import io.jaegertracing.api_v2.query.TraceQueryParameters
import zio.{IO, ZIO, ZLayer}

import java.util.UUID

trait ApiService {
  def getCluster(id: ApiClusterId): IO[DeepTraceError, ApiCluster]
  def createReport(request: ApiAnalysisRequest): IO[DeepTraceError, ApiReport]
  def listReports(): IO[DeepTraceError, List[ApiReport]]
  def getReport(id: String): IO[DeepTraceError, ApiReport]
}

object ApiService {

  def getCluster(id: ApiClusterId): ZIO[ApiService, DeepTraceError, ApiCluster] =
    ZIO.serviceWithZIO[ApiService](_.getCluster(id))

  def createReport(request: ApiAnalysisRequest): ZIO[ApiService, DeepTraceError, ApiReport] =
    ZIO.serviceWithZIO[ApiService](_.createReport(request))

  def listReports(): ZIO[ApiService, DeepTraceError, List[ApiReport]] =
    ZIO.serviceWithZIO[ApiService](_.listReports())

  def getReport(id: String): ZIO[ApiService, DeepTraceError, ApiReport] =
    ZIO.serviceWithZIO[ApiService](_.getReport(id))

  class Live(
      engine: Engine,
      reportStore: TaskStore,
      clusterStore: ClusterStore,
      jaegerSource: JaegerSource)
    extends ApiService {

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

    override def listReports(): IO[DeepTraceError, List[ApiReport]] =
      reportStore.list().map(toApi).runCollect.map(_.toList)

    override def getReport(id: String): IO[DeepTraceTaskNotFound, ApiReport] =
      reportStore.get(UUID.fromString(id)).map(toApi)
  }

  val live: ZLayer[
    Engine with TaskStore with ClusterStore with JaegerSource,
    Nothing,
    ApiService
  ] = {
    ZLayer {
      for {
        engine <- ZIO.service[Engine]
        reportStore <- ZIO.service[TaskStore]
        clusterStore <- ZIO.service[ClusterStore]
        jaegerSource <- ZIO.service[JaegerSource]
      } yield new Live(engine, reportStore, clusterStore, jaegerSource)
    }
  }
}
