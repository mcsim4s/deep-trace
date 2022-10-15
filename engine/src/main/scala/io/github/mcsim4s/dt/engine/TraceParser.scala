package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.Process.ParallelProcess
import io.github.mcsim4s.dt.model.{Process, ProcessInstance, RawTrace}
import zio._
import zio.macros.accessible
import zio.stream._

@accessible
object TraceParser {
  type TraceParser = Service

  case class TraceParsingState(
      process: ParallelProcess,
      instances: Seq[ProcessInstance],
      current: ProcessInstance.Single,
      containsErrors: Boolean,
      exampleId: String
  )

  trait Service {
    def parse(rawTrace: RawTrace, operationName: String): Stream[RawTraceMappingError, TraceParsingState]
  }
}
