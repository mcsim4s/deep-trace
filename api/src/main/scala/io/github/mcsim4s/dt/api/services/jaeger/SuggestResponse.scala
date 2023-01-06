package io.github.mcsim4s.dt.api.services.jaeger

case class SuggestResponse(services: Seq[String], operations: Seq[OperationSuggest], forService: Option[String])
