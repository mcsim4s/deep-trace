package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.TraceCluster.ClusterId

sealed class DeepTraceError(msg: String) {
  def message = msg
}

object DeepTraceError {
  case class RawTraceMappingError(msg: String) extends DeepTraceError(msg)

  case class TraceRetrieveError(msg: String) extends DeepTraceError(msg)

  case class ReportNotFound(id: String) extends DeepTraceError(s"Analysis report with id: $id not found")

  case class ClusterNotFound(id: ClusterId) extends DeepTraceError(s"Cluster with id: $id not found")

  case class CasConflict(entityType: String, id: String) extends DeepTraceError(s"$entityType update conflict for $id")
}
