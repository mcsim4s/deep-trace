addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")

val zioGrpcVersion = "0.5.1"

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.7",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
)
