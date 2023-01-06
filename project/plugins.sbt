addSbtPlugin("org.jetbrains"    % "sbt-ide-settings"     % "1.1.0")
addSbtPlugin("com.thesamet"     % "sbt-protoc"           % "1.0.3")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"         % "0.9.34")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

val zioGrpcVersion = "0.6.0-test4"

libraryDependencies ++= Seq(
  "com.thesamet.scalapb"          %% "compilerplugin"   % "0.11.12",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
)
