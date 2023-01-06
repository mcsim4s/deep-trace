name := "deep-trace"

version := "0.1"

ThisBuild / scalaVersion := "2.13.7"

// ***************************
// Projects
// ***************************

val toolkit = (project in file("toolkit"))
  .settings(commonSettings)
  .settings(
    name := "toolkit",
    libraryDependencies ++= Seq(
      library.zio,
      library.zioTest    % Test,
      library.zioTestSbt % Test
    ) ++ library.logging
  )

val jaegerModel = (project in file("jaeger-model"))
  .settings(commonSettings)
  .settings(
    name := "jaeger-model",
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
    ),
    libraryDependencies ++= library.protobuf,
    dependencyOverrides ++= library.protobufOverrides ++ Seq(
      library.zio,
      library.zioStreams
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
      library.zioTest    % Test,
      library.zioTestSbt % Test
    ),
    dependencyOverrides ++= library.protobufOverrides ++ Seq(
      library.zio,
      library.zioStreams
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
    ) ++ library.circe,
    dependencyOverrides ++= library.protobufOverrides ++ Seq(
      library.zio,
      library.zioStreams
    )
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
      library.calibanHttp4s
    )
  )
  .dependsOn(toolkit, engine)

// ***************************
// Dependencies
// ***************************

import scalapb.compiler.{Version => VersionPb}

lazy val library =
  new {

    object Version {
      val openTelemetryVersion = "1.21.0"
      val zioVersion = "2.0.5"
      val zioLoggingVersion = "2.1.7"
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

//      protobuf stuff
      val guavaVersion = "30.1.1-android"
      val grpcCoreVersion = "1.51.0"
      val scalaPbCompilerVersion = "0.11.12"
    }

    val openTelemetry =
      "io.opentelemetry"                         % "opentelemetry-api"      % Version.openTelemetryVersion
    val zio = "dev.zio"                         %% "zio"                    % Version.zioVersion
    val zioStreams = "dev.zio"                  %% "zio-streams"            % Version.zioVersion
    val zioTest = "dev.zio"                     %% "zio-test"               % Version.zioVersion
    val zioTestSbt = "dev.zio"                  %% "zio-test-sbt"           % Version.zioVersion
    val grpc = "io.grpc"                         % "grpc-netty"             % Version.grpcVersion
    val caliban = "com.github.ghostdogpr"       %% "caliban"                % Version.calibanVersion
    val calibanHttp4s = "com.github.ghostdogpr" %% "caliban-http4s"         % Version.calibanVersion
    val math = "org.apache.commons"              % "commons-math3"          % Version.math
    val doobie = "org.tpolecat"                 %% "doobie-core"            % Version.doobieVersion
    val doobiePostgres = "org.tpolecat"         %% "doobie-postgres"        % Version.doobieVersion
    val doobiePostgresCirce = "org.tpolecat"    %% "doobie-postgres-circe"  % Version.doobieVersion
    val zioInteropCats = "dev.zio"              %% "zio-interop-cats"       % Version.zioCatsInteropVersion
    val pureConfig = "com.github.pureconfig"    %% "pureconfig"             % Version.pureConfigVersion
    val clickHouseDriver = "com.clickhouse"      % "clickhouse-jdbc"        % Version.clickhouseVersion
    val clickHouseHttpClient = "com.clickhouse"  % "clickhouse-http-client" % Version.clickhouseVersion
    val clickHouseClient = "com.clickhouse"      % "clickhouse-client"      % Version.clickhouseVersion
    val hikariCP = "com.zaxxer"                  % "HikariCP"               % Version.hikariCpVersion
    val http4sDsl = "org.http4s"                %% "http4s-dsl"             % Version.http4sVersion
    val http4sServer = "org.http4s"             %% "http4s-blaze-server"    % Version.blazeVersion

    val circe = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic"
    ).map(_ % Version.circeVersion).map(_.exclude("org.typelevel", "cats-core_2.13"))

    val logging = Seq(
      "org.slf4j"       % "slf4j-api"         % Version.slf4jVersion,
      "dev.zio"        %% "zio-logging"       % Version.zioLoggingVersion,
      ("dev.zio"       %% "zio-logging-slf4j" % Version.zioLoggingVersion).exclude("org.slf4j", "slf4j-api"),
      ("ch.qos.logback" % "logback-classic"   % Version.logbackVersion).exclude("org.slf4j", "slf4j-api")
    )

    val protobufOverrides = Seq(
      "com.google.guava"      % "guava"                   % Version.guavaVersion,
      "io.grpc"               % "grpc-core"               % Version.grpcCoreVersion,
      "io.grpc"               % "grpc-api"                % Version.grpcCoreVersion,
      "io.grpc"               % "grpc-stub"               % Version.grpcCoreVersion,
      "io.grpc"               % "grpc-protobuf"           % Version.grpcCoreVersion,
      "com.google.errorprone" % "error_prone_annotations" % "2.17.0",
      "com.google.protobuf"   % "protobuf-java"           % "3.21.12",
      "com.google.code.gson"  % "gson"                    % "2.10"
    ).map(_.force())

    val protobuf = Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % VersionPb.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime"      % VersionPb.scalapbVersion % "protobuf"
    )
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
