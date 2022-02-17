package io.github.mcsim4s.dt.api

import caliban.{CalibanError, ZHttpAdapter}
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.live.LiveEngine
import io.github.mcsim4s.dt.engine.live.store.{LiveClusterStore, LiveProcessStore, LiveReportStore, LiveSpanStore}
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.github.mcsim4s.dt.engine.store.ClusterStore.ClusterStore
import io.grpc.ManagedChannelBuilder
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import scalapb.zio_grpc.ZManagedChannel
import zhttp.http.Method.OPTIONS
import zhttp.http.Middleware.cors
import zhttp.http._
import zhttp.service.Server
import zio.blocking.Blocking
import zio.stream.ZStream
import zio._
import zio.console.Console
import zio.magic._

object Main extends zio.App {
  type Env = ZEnv with Engine with Has[JaegerSource] with ClusterStore with ApiService

  val serverPort = 8080

  private def graphiql = {
    ZIO.environment[Blocking].map { blocking =>
      Response(
        headers = Headers(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML),
        data = HttpData.fromStream(ZStream.fromResource("graphiql.html").provide(blocking))
      )
    }
  }

  val route: IO[CalibanError.ValidationError, Http[Env, Throwable, Request, Response]] =
    Api.root.interpreter.map { api =>
      Http
        .route {
          case method -> !! / "graphql" if method != OPTIONS => ZHttpAdapter.makeHttpService(api)
          case _ -> !! / "graphiql"                          => Http.responseZIO(graphiql)
          case _ -> !!                                       => Http.response(Response.redirect("/graphiql", isPermanent = true))
        }
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val program = route.flatMap { route =>
      zio.console.putStrLn(s"Server started on http://localhost:$serverPort") *>
        Server
          .start(
            serverPort,
            route @@ cors()
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

    ZLayer.wireSome[ZEnv, Env](
      jaegerClient,
      JaegerSource.layer,
      LiveReportStore.layer,
      LiveClusterStore.layer,
      LiveProcessStore.layer,
      LiveSpanStore.layer,
      LiveEngine.layer,
      ApiService.live
    )

  }
}
