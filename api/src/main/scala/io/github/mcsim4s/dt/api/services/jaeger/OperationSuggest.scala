package io.github.mcsim4s.dt.api.services.jaeger

import io.jaegertracing.api_v2.query.Operation

case class OperationSuggest(name: String, kind: String)

object OperationSuggest {
  def apply(operationProto: Operation): OperationSuggest =
    OperationSuggest(
      name = operationProto.name,
      kind = operationProto.spanKind
    )
}
