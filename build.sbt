name := "deep-trace"

version := "0.1"

ThisBuild / scalaVersion := "2.13.7"

// ***************************
// Projects
// ***************************

val jaegerModel = (project in file("jaeger-model"))
  .settings(
    name := "jaeger-model",
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      library.scalaPbRuntime,
      library.scalaPbRuntimeGrpc
    )
  )

val model = (project in file("model"))
  .settings(commonSettings)
  .settings(
    name := "model",
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      library.openTelemetry,
      library.zio,
      library.zioTest    % Test,
      library.zioTestSbt % Test
    )
  )
  .dependsOn(jaegerModel)

val dao = (project in file("dao"))
  .settings(commonSettings)
  .settings(
    name := "dao",
    libraryDependencies ++= Seq(
      library.zio,
      library.doobie,
      library.doobiePostgres,
      library.doobiePostgresCirce,
      library.zioInteropCats,
      library.hikariCP,
      library.clickHouseDriver,
      library.zioTest    % Test,
      library.zioTestSbt % Test
    ) ++ library.circe
  )
  .dependsOn(model)

val engine = (project in file("engine"))
  .settings(commonSettings)
  .settings(
    name := "engine",
    libraryDependencies ++= Seq(
      library.zio,
      library.zioStreams,
      library.math,
      library.zioTest    % Test,
      library.zioTestSbt % Test
    )
  )
  .dependsOn(dao)

val api = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      library.grpc,
      library.http4sDsl,
      library.http4sServer,
      library.pureConfig,
      library.caliban,
      library.slf4jApi,
      library.julToSlf4j,
      library.logback,
      library.calibanHttp4s
    )
  )
  .dependsOn(engine)

// ***************************
// Dependencies
// ***************************

import scalapb.compiler.{Version => VersionPb}

lazy val library =
  new {

    object Version {
      val openTelemetryVersion = "1.21.0"
      val zioVersion = "2.0.5"
      val grpcVersion = "1.51.0"
      val calibanVersion = "2.0.2"
      val math = "3.6.1"
      val doobieVersion = "1.0.0-RC2"
      val zioCatsInteropVersion = "23.0.0.0"
      val pureConfigVersion = "0.17.2"
      val clickhouseVersion = "0.3.2-patch11"
      val hikariCpVersion = "5.0.1"
      val http4sVersion = "0.23.16"
      val blazeVersion = "0.23.13"
      val circeVersion = "0.14.1"
      val slf4jVersion = "2.0.5"
      val logbackVersion = "1.4.5"
    }

    val openTelemetry =
      "io.opentelemetry"                         % "opentelemetry-api" % Version.openTelemetryVersion
    val zio = "dev.zio"                         %% "zio"               % Version.zioVersion
    val zioStreams = "dev.zio"                  %% "zio-streams"       % Version.zioVersion
    val zioTest = "dev.zio"                     %% "zio-test"          % Version.zioVersion
    val zioTestSbt = "dev.zio"                  %% "zio-test-sbt"      % Version.zioVersion
    val grpc = "io.grpc"                         % "grpc-netty"        % Version.grpcVersion
    val caliban = "com.github.ghostdogpr"       %% "caliban"           % Version.calibanVersion
    val calibanHttp4s = "com.github.ghostdogpr" %% "caliban-http4s"    % Version.calibanVersion

    val scalaPbRuntime =
      "com.thesamet.scalapb" %% "scalapb-runtime" % VersionPb.scalapbVersion % "protobuf"

    val scalaPbRuntimeGrpc =
      "com.thesamet.scalapb"                   %% "scalapb-runtime-grpc"   % VersionPb.scalapbVersion
    val math = "org.apache.commons"             % "commons-math3"          % Version.math
    val doobie = "org.tpolecat"                %% "doobie-core"            % Version.doobieVersion
    val doobiePostgres = "org.tpolecat"        %% "doobie-postgres"        % Version.doobieVersion
    val doobiePostgresCirce = "org.tpolecat"   %% "doobie-postgres-circe"  % Version.doobieVersion
    val zioInteropCats = "dev.zio"             %% "zio-interop-cats"       % Version.zioCatsInteropVersion
    val pureConfig = "com.github.pureconfig"   %% "pureconfig"             % Version.pureConfigVersion
    val clickHouseDriver = "com.clickhouse"     % "clickhouse-jdbc"        % Version.clickhouseVersion
    val clickHouseHttpClient = "com.clickhouse" % "clickhouse-http-client" % Version.clickhouseVersion
    val clickHouseClient = "com.clickhouse"     % "clickhouse-client"      % Version.clickhouseVersion
    val hikariCP = "com.zaxxer"                 % "HikariCP"               % Version.hikariCpVersion
    val http4sDsl = "org.http4s"               %% "http4s-dsl"             % Version.http4sVersion
    val http4sServer = "org.http4s"            %% "http4s-blaze-server"    % Version.blazeVersion
    val slf4jApi = "org.slf4j"                  % "slf4j-api"              % Version.slf4jVersion
    val julToSlf4j = "org.slf4j"                % "jul-to-slf4j"           % Version.slf4jVersion
    val logback = "ch.qos.logback"              % "logback-classic"        % Version.logbackVersion

    val circe = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % Version.circeVersion)

  }

// ***************************
// Settings
// ***************************

lazy val commonSettings =
  Seq(
    licenses += ("Apache-2.0", new URL(
      "https://www.apache.org/licenses/LICENSE-2.0.txt"
    )),
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      "-deprecation"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
