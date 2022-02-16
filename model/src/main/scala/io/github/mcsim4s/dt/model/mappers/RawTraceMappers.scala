package io.github.mcsim4s.dt.model.mappers

import com.google.protobuf.ByteString
import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.{Process, RawTrace, Trace}
import io.github.mcsim4s.dt.model._
import io.jaegertracing.api_v2.model.Span
import zio.{IO, Ref, ZIO}

import java.time.Instant

object RawTraceMappers {
  type RawSpanId = ByteString
  type ParentToChild = Map[RawSpanId, Seq[RawSpanId]]

  def fromRaw(rawTrace: RawTrace): IO[RawTraceMappingError, Process] = {
    def updateRootRef(ref: Ref[Option[Span]], span: Span) =
      ref.set(Some(span)).when(!span.references.exists(_.refType.isChildOf))

    def updateParentChildMap(ref: Ref[ParentToChild], span: Span) =
      ZIO.foreach_(span.references.filter(_.refType.isChildOf))(r => ref.update(_.append(r.spanId, span.spanId)))

    for {
      rootRef <- Ref.make[Option[Span]](None)
      parentToChildMapRef <- Ref.make[ParentToChild](Map.empty)

      spans <-
        ZIO
          .foreach(rawTrace.spans) { span =>
            updateRootRef(rootRef, span) *> updateParentChildMap(parentToChildMapRef, span).as(span.spanId -> span)
          }
          .map(_.toMap)

      rootSpan <-
        rootRef.get
          .flatMap(opt => ZIO.fromOption(opt))
          .orElseFail(RawTraceMappingError("Trace source didn't contain root span(span without childOf relation)"))
      parentToChildMap <- parentToChildMapRef.get
      rootProcess <- fromSpan(rootSpan, spans, parentToChildMap, rootSpan.getStartTime.toInstant)
    } yield rootProcess
  }

  def fromSpan(
      span: Span,
      spans: Map[RawSpanId, Span],
      parentToChildMap: ParentToChild,
      startTime: Instant
  ): IO[RawTraceMappingError, Process] =
    for {
      children <- ZIO.foreach(parentToChildMap.getOrElse(span.spanId, Seq.empty)) { child =>
        fromSpan(spans(child), spans, parentToChildMap, startTime)
      }
    } yield Process(
      name = span.operationName,
      start = span.getStartTime.toInstant.minus(startTime).toDuration,
      duration = span.getDuration.asScala,
      children = children
    )
}
