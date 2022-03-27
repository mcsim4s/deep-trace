package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.engine.TraceParser
import io.github.mcsim4s.dt.engine.TraceParser.{ParentToChild, RawSpanId, TraceParser}
import io.github.mcsim4s.dt.engine.store.SpanStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.model
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.{DeepTraceError, Process, RawTrace, _}
import io.jaegertracing.api_v2.model.Span
import zio._
import zio.random.Random

import java.time.Instant

class TraceParserLive(spanStore: SpanStore.Service, random: Random.Service) extends TraceParser.Service {
  override def parse(rawTrace: RawTrace): IO[DeepTraceError.RawTraceMappingError, model.Process] = {
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
      result = Process(
        service = span.getProcess.serviceName,
        operation = span.operationName,
        children = children
      )
      start = span.getStartTime.toInstant.minus(startTime).toTimeStamp
      fixedStart = span.copy(startTime = Some(start))
      _ <- spanStore.add(result.id, fixedStart)
    } yield result
}

object TraceParserLive {
  val layer: URLayer[SpanStore with Random, TraceParser] =
    ZLayer.fromServices[SpanStore.Service, Random.Service, TraceParser.Service](new TraceParserLive(_, _))
}
