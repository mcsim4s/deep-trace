package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.engine.TraceParser._
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.Process.ParallelProcess
import io.github.mcsim4s.dt.model.{ProcessInstance, RawTrace}
import zio.stream._

trait TraceParser {
  def parse(rawTrace: RawTrace, operationName: String): Stream[RawTraceMappingError, TraceParsingState]
}

object TraceParser {
  def parse(rawTrace: RawTrace, operationName: String): ZStream[TraceParser, RawTraceMappingError, TraceParsingState] =
    ZStream.serviceWithStream[TraceParser](_.parse(rawTrace, operationName))

  case class TraceParsingState(
      process: ParallelProcess,
      instances: Seq[ProcessInstance],
      current: ProcessInstance.Single,
      containsErrors: Boolean,
      exampleId: String
  )

}
