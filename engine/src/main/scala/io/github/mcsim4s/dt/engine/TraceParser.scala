package io.github.mcsim4s.dt.engine

import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.{Process, RawTrace}
import zio._
import zio.macros.accessible
import zio.stream._

@accessible
object TraceParser {
  type TraceParser = Has[Service]

  trait Service {
    def parse(rawTrace: RawTrace, operationName: String): Stream[RawTraceMappingError, Process]
  }
}
