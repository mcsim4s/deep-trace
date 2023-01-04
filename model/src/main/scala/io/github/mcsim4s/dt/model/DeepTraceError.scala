package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.TraceCluster.ClusterId
import io.grpc.Status

import java.util.UUID
import scala.language.implicitConversions

sealed trait DeepTraceError {
  def message: String
}

object DeepTraceError {
  case class GenericError(message: String) extends DeepTraceError

  case class RawTraceMappingError(message: String) extends DeepTraceError

  case class TraceRetrieveError(message: String) extends DeepTraceError

  case class ExternalGrpcError(service: String, status: Status) extends DeepTraceError {
    val message = s"Grpc service '$service' call error. Status: $status"
  }

  case class DeepTraceTaskNotFound(id: UUID) extends DeepTraceError {
    val message = s"Analysis report with id: $id not found"
  }

  case class ClusterNotFound(id: ClusterId) extends DeepTraceError {
    val message = s"Cluster with id: $id not found"
  }

  case class CasConflict(entityType: String, id: String) extends DeepTraceError {
    val message = s"$entityType update conflict for $id"
  }

  case class UnexpectedDbError(operationName: String, cause: Throwable) extends DeepTraceError {
    override val message: String = s"Db operation '$operationName' failed. Cause message: '${cause.getMessage}'"
  }
}
