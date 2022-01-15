package io.github.mcsim4s.dt.api

import caliban.ZHttpAdapter
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.live.LiveEngine
import io.github.mcsim4s.dt.engine.live.store.LiveReportStore
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.grpc.ManagedChannelBuilder
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import scalapb.zio_grpc.ZManagedChannel
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.magic._

object Main extends zio.App {
  type Env = ZEnv with Engine with Has[JaegerSource] with ClusterStore

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program = Api.root.interpreter.flatMap { api =>
      Server
        .start(
          8080,
          Http.route {
            case _ -> Root / "api" => ZHttpAdapter.makeHttpService(api)
          }
        )
        .forever
    }
    program.provideLayer(liveLayer).exitCode
  }

  lazy val liveLayer: ZLayer[ZEnv, Throwable, Env] = {
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
