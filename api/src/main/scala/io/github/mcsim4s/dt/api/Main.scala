package io.github.mcsim4s.dt.api

import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter}
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService.JaegerService
import io.github.mcsim4s.dt.dao.impl.LiveTaskDao
import io.github.mcsim4s.dt.dao.impl.LiveTaskDao.TaskDaoConf
import io.github.mcsim4s.dt.engine.live.store.{LiveClusterStore, LiveProcessStore, LiveTaskStore}
import io.github.mcsim4s.dt.engine.live.{LiveEngine, TraceParserLive}
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.grpc.ManagedChannelBuilder
import io.jaegertracing.api_v2.query.ZioQuery.QueryServiceClient
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.request._
import org.http4s.headers.Location
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import scalapb.zio_grpc.ZManagedChannel
import zio._
import zio.stream.ZStream
import pureconfig.generic.auto._
import zio.interop.catz._
import zio.stream.interop.fs2z._

object Main extends zio.ZIOAppDefault {
  type Env = ApiService with JaegerService
  type GQL = GraphQLInterpreter[Env, CalibanError]
  type ApiTask[+A] = ZIO[Env, Throwable, A]

  private val serverPort: RuntimeFlags = 8080

  private def graphiql: Response[ApiTask] = {
    Response(
      headers = Headers(Header("content-type", "text/html")),
      body = ZStream.fromResource("graphiql.html").toFs2Stream
    )
  }

  val staticRoutes: HttpRoutes[ApiTask] =
    HttpRoutes.of[ApiTask] {
      case _ -> Root / "graphiql" => ZIO.succeed(graphiql)
      case _ =>
        ZIO.succeed(
          Response[ApiTask]()
            .withStatus(Status.Found)
            .withHeaders(Location(Uri()./("graphiql")))
        )
    }

  private def httpApp(api: GQL): HttpApp[ApiTask] = Router[ApiTask](
    "/graphql" -> CORS.policy(Http4sAdapter.makeHttpService(api)),
    "/" -> staticRoutes
  ).orNotFound

  override def run: RIO[ZIOAppArgs with Scope, Any] = {
    val program = ZIO.service[GQL].flatMap { api =>
      BlazeServerBuilder[ApiTask]
        .bindLocal(serverPort)
        .withHttpApp(httpApp(api))
        .withoutSsl
        .serve
        .compile
        .drain
    }
    program
      .provideSome(
        jaegerClient,
        Pureconfig.load[TaskDaoConf]("task-dao"),
        LiveTaskDao.layer,
        JaegerSource.layer,
        LiveTaskStore.layer,
        LiveClusterStore.layer,
        LiveProcessStore.layer,
        TraceParserLive.layer,
        LiveEngine.layer,
        ApiService.live,
        JaegerService.layer,
        ZLayer.apply(Api.root.interpreter)
      )
      .absorb
  }

  lazy val jaegerClient: Layer[Throwable, QueryServiceClient] = QueryServiceClient.live(
    ZManagedChannel {
      ManagedChannelBuilder.forAddress("localhost", 16685).usePlaintext()
    }
  )
}
