name := "deep-trace"

version := "0.1"

ThisBuild / scalaVersion := "3.1.0"
ThisBuild / idePackagePrefix.withRank(KeyRanks.Invisible) := Some(
  "io.github.mcsim4s.dt"
)
ThisBuild / idePackagePrefix := Some("io.github.mcsim4s.dt")

val openTracingVersion = "0.33.0"

val model = (project in file("model"))
  .settings(
    name := "model",
    libraryDependencies ++= Seq(
      "io.opentracing" % "opentracing-api" % openTracingVersion
    )
  )
