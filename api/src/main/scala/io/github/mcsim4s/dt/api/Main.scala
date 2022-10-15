package io.github.mcsim4s.dt.api

import caliban.{CalibanError, ZHttpAdapter}
import io.github.mcsim4s.dt.api.ApiService.ApiService
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService.JaegerService
import io.github.mcsim4s.dt.engine.Engine.Engine
import io.github.mcsim4s.dt.engine.live.store.{LiveClusterStore, LiveProcessStore, LiveReportStore}
import io.github.mcsim4s.dt.engine.live.{LiveEngine, TraceParserLive}
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
import zio._
import zio.stream.ZStream

object Main extends zio.ZIOAppDefault {
  type Env = Engine with JaegerSource with ClusterStore with ApiService with JaegerService

  val serverPort = 8080

  private def graphiql = {
    Response(
      headers = Headers(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML),
      data = HttpData.fromStream(ZStream.fromResource("graphiql.html"))
    )
  }

  val route: IO[CalibanError.ValidationError, Http[Env, Throwable, Request, Response]] =
    Api.root.interpreter.map { api =>
      Http.collectHttp {
        case method -> !! / "graphql" if method != OPTIONS => ZHttpAdapter.makeHttpService(api)
        case _ -> !! / "graphiql"                          => Http.response(graphiql)
        case _ -> !!                                       => Http.response(Response.redirect("/graphiql", isPermanent = true))
      }
    }

  override def run: URIO[Any, ExitCode] = {
    val program = route.flatMap { route =>
      zio.Console.printLine(s"Server started on http://localhost:$serverPort") *>
        Server
          .start(
            serverPort,
            route @@ cors()
          )
          .forever
    }
    program
      .provide(
        jaegerClient,
        JaegerSource.layer,
        LiveReportStore.layer,
        LiveClusterStore.layer,
        LiveProcessStore.layer,
        TraceParserLive.layer,
        LiveEngine.layer,
        ApiService.live,
        JaegerService.layer
      )
      .exitCode
  }

  lazy val jaegerClient: Layer[Throwable, QueryServiceClient] = QueryServiceClient.live(
    ZManagedChannel(
      ManagedChannelBuilder.forAddress("localhost", 16685).usePlaintext()
    )
  )
}
