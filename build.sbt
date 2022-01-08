name := "deep-trace"

version := "0.1"

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / idePackagePrefix := Some("io.github.mcsim4s.dt")
ThisBuild / idePackagePrefix := Some("io.github.mcsim4s.dt")

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
    libraryDependencies ++= Seq(
      library.openTelemetry,
      library.zio,
      library.zioTest,
      library.zioTestSbt
    )
  )
  .dependsOn(jaegerModel)

val engine = (project in file("engine"))
  .settings(commonSettings)
  .settings(
    name := "engine",
    libraryDependencies ++= Seq(
      library.zio,
      library.zioStreams,
      library.zioMacro
    )
  )
  .dependsOn(model)

val api = (project in file("api"))
  .settings(commonSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      library.grpc
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
      val openTelemetryVersion = "1.9.1"
      val zioVersion = "1.0.13"
      val grpcVersion = "1.43.2"
    }
    val openTelemetry =
      "io.opentelemetry" % "opentelemetry-api" % Version.openTelemetryVersion
    val zio = "dev.zio" %% "zio" % Version.zioVersion
    val zioStreams = "dev.zio" %% "zio-streams" % Version.zioVersion
    val zioMacro = "dev.zio" %% "zio-macros" % Version.zioVersion
    val zioTest = "dev.zio" %% "zio-test" % Version.zioVersion
    val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zioVersion
    val grpc = "io.grpc" % "grpc-netty" % Version.grpcVersion

    val scalaPbRuntime =
      "com.thesamet.scalapb" %% "scalapb-runtime" % VersionPb.scalapbVersion % "protobuf"

    val scalaPbRuntimeGrpc =
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % VersionPb.scalapbVersion
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
      "-Ymacro-annotations"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
