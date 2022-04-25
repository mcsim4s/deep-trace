package io.github.mcsim4s.dt.api.model

case class TraceCluster(id: ClusterId, exampleTraceId: String, processes: Map[String, Process])
