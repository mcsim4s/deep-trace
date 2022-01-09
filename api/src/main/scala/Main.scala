package io.github.mcsim4s.dt

import Engine.Engine
import source.JaegerSource

import com.google.protobuf.timestamp.Timestamp
import io.github.mcsim4s.dt.live.LiveEngine
import io.github.mcsim4s.dt.live.store.LiveReportStore
import io.grpc.ManagedChannelBuilder
import io.jaegertracing.api_v2.query.TraceQueryParameters
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import scalapb.zio_grpc.ZManagedChannel
import zio._
import zio.magic._
import zio.clock.Clock

object Main extends zio.App {
  type Env = ZEnv with Engine with Has[JaegerSource]

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program: ZIO[Env, Object, AnalysisReport] =
      for {
        now <- ZIO.accessM[Clock](_.get.instant)
        request <-
          ZIO
            .service[JaegerSource]
            .flatMap(
              _.createTask(
                TraceQueryParameters(
                  operationName = "/api/traces",
                  startTimeMin = Some(Timestamp.of(now.minusSeconds(60 * 60 * 2).getEpochSecond, 0)),
                  startTimeMax = Some(Timestamp.of(now.getEpochSecond, 0))
                )
              )
            )
        report <- Engine.process(request)
      } yield report

    program.provideLayer(liveLayer).exitCode
  }

  lazy val liveLayer: ZLayer[ZEnv, Throwable, ZEnv with Engine with Has[JaegerSource]] = {
    val jaegerClient: Layer[Throwable, QueryServiceClient] = QueryServiceClient.live(
      ZManagedChannel(
        ManagedChannelBuilder.forAddress("localhost", 16685).usePlaintext()
      )
    )

    ZLayer.fromSomeMagic[ZEnv, Env](
      jaegerClient,
      JaegerSource.layer,
      LiveReportStore.layer,
      LiveEngine.layer
    )
  }
}
