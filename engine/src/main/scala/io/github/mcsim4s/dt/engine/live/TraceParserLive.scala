package io.github.mcsim4s.dt.engine.live

import com.google.protobuf.ByteString
import io.github.mcsim4s.dt.engine.TraceParser
import io.github.mcsim4s.dt.engine.TraceParser.TraceParser
import io.github.mcsim4s.dt.engine.live.TraceParserLive.{ParentToChild, RawSpanId}
import io.github.mcsim4s.dt.engine.store.SpanStore
import io.github.mcsim4s.dt.engine.store.SpanStore.SpanStore
import io.github.mcsim4s.dt.model
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.{DeepTraceError, Process, RawTrace, _}
import io.jaegertracing.api_v2.model.Span
import zio._
import zio.random.Random
import zio.stream._

import java.time.Instant

class TraceParserLive(spanStore: SpanStore.Service, random: Random.Service) extends TraceParser.Service {

  override def parse(
      rawTrace: RawTrace,
      operationsName: String
  ): Stream[DeepTraceError.RawTraceMappingError, model.Process] = {
    def pushRoot(ref: Ref[Seq[Span]], span: Span) =
      ref.update(_ :+ span)

    def updateParentChildMap(ref: Ref[ParentToChild], span: Span) =
      ZIO.foreach_(span.references.filter(_.refType.isChildOf))(r => ref.update(_.append(r.spanId, span)))

    val parseOne = for {
      rootRefs <- Ref.make[Seq[Span]](Seq.empty)
      parentToChildMapRef <- Ref.make[ParentToChild](Map.empty)

      _ <-
        ZIO
          .foreach(rawTrace.spans) { span =>
            pushRoot(rootRefs, span)
              .when(!span.references.exists(_.refType.isChildOf)) *>
              updateParentChildMap(parentToChildMapRef, span).as(span.spanId -> span)
          }
          .map(_.toMap)

      rootSpan <-
        rootRefs.get
          .flatMap(set => ZIO.fromOption(set.headOption))
          .orElseFail(RawTraceMappingError("Trace source didn't contain root span(span without childOf relation)"))
      parentToChildMap <- parentToChildMapRef.get
      _ <-
        rootRefs.get
          .flatMap { candidates =>
            val newStack = candidates.flatMap { c =>
              if (c.operationName == operationsName)
                Seq(c)
              else
                parentToChildMap.getOrElse(c.spanId, Seq.empty)
            }
            rootRefs.set(newStack)
          }
          .repeatWhileM(_ => rootRefs.get.map(_.exists(_.operationName != operationsName)))
      roots <- rootRefs.get
      parsed <- ZIO.foreach(roots)(r => fromSpan(r, parentToChildMap, rootSpan.getStartTime.toInstant))
    } yield parsed

    ZStream.fromEffect(parseOne).flatMap(chunk => ZStream.fromIterable(chunk))
  }

  def fromSpan(
      span: Span,
      parentToChildMap: ParentToChild,
      startTime: Instant
  ): IO[RawTraceMappingError, Process] =
    for {
      children <- ZIO.foreach(parentToChildMap.getOrElse(span.spanId, Seq.empty)) { child =>
        fromSpan(child, parentToChildMap, startTime)
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
  type RawSpanId = ByteString
  type ParentToChild = Map[RawSpanId, Seq[Span]]

  val layer: URLayer[SpanStore with Random, TraceParser] =
    ZLayer.fromServices[SpanStore.Service, Random.Service, TraceParser.Service](new TraceParserLive(_, _))
}
