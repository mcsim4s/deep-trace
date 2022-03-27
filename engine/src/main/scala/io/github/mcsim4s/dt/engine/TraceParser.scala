package io.github.mcsim4s.dt.engine

import com.google.protobuf.ByteString
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.{Process, RawTrace}
import zio._
import zio.macros.accessible

@accessible
object TraceParser {
  type TraceParser = Has[Service]
  type RawSpanId = ByteString
  type ParentToChild = Map[RawSpanId, Seq[RawSpanId]]

  trait Service {
    def parse(rawTrace: RawTrace): IO[RawTraceMappingError, Process]
  }
}
