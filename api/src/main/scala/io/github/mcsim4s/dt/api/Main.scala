package io.github.mcsim4s.dt.api

import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter}
import io.github.mcsim4s.dt.api.services.jaeger.JaegerService
import io.github.mcsim4s.dt.dao.impl.LiveReportDao
import io.github.mcsim4s.dt.engine.Engine
import io.github.mcsim4s.dt.engine.live.store.{LiveClusterStore, LiveProcessStore, LiveReportStore}
import io.github.mcsim4s.dt.engine.live.{LiveEngine, TraceParserLive}
import io.github.mcsim4s.dt.engine.source.JaegerSource
import io.github.mcsim4s.toolkit.app.BaseApplication
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
import zio.interop.catz._
import zio.stream.ZStream
import zio.stream.interop.fs2z._

object Main extends BaseApplication {
  private type GQL = GraphQLInterpreter[Api.Environment, CalibanError]

  override type ApplicationEnvironment = Environment with Api.Environment with GQL with Engine
  type ApiTask[+A] = ZIO[Api.Environment, Throwable, A]

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

  private def httpApp(api: GQL): HttpApp[ApiTask] =
    Router[ApiTask](
      "/graphql" -> CORS.policy(Http4sAdapter.makeHttpService(api)),
      "/" -> staticRoutes
    ).orNotFound

  private val startGraphQLServer = ZIO.service[GQL].flatMap { api =>
    BlazeServerBuilder[ApiTask]
      .bindLocal(serverPort)
      .withHttpApp(httpApp(api))
      .withoutSsl
      .serve
      .compile
      .drain
  }

  override def program: ZIO[ApplicationEnvironment, Throwable, Unit] =
    (startGraphQLServer <&>
      ZIO
        .serviceWithZIO[Engine](_.start)
        .orDieWith(err => new IllegalStateException(err.message))).unit

  override def applicationEvn: ZLayer[Environment, Throwable, ApplicationEnvironment] =
    ZLayer.makeSome[Environment, ApplicationEnvironment](
      jaegerClient,
      LiveReportDao.layer,
      JaegerSource.layer,
      LiveReportStore.layer,
      LiveClusterStore.layer,
      LiveProcessStore.layer,
      TraceParserLive.layer,
      LiveEngine.layer,
      ApiService.live,
      JaegerService.layer,
      ZLayer.apply(Api.root.interpreter)
    )

  lazy val jaegerClient: Layer[Throwable, QueryServiceClient] = QueryServiceClient.live(
    ZManagedChannel {
      ManagedChannelBuilder.forAddress("localhost", 16685).usePlaintext().asInstanceOf[ManagedChannelBuilder[_]]
    }
  )
}
