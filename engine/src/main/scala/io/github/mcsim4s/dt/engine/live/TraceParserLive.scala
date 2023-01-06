package io.github.mcsim4s.dt.engine.live

import com.google.protobuf.ByteString
import io.github.mcsim4s.dt.engine.TraceParser
import io.github.mcsim4s.dt.engine.TraceParser._
import io.github.mcsim4s.dt.engine.live.TraceParserLive._
import io.github.mcsim4s.dt.engine.store.ProcessStore
import io.github.mcsim4s.dt.model.DeepTraceError.RawTraceMappingError
import io.github.mcsim4s.dt.model.Process._
import io.github.mcsim4s.dt.model._
import io.jaegertracing.api_v2.model.Span
import zio.stream._
import zio.telemetry.opentelemetry.Tracing
import zio.{IO, Ref, URLayer, ZIO, ZLayer}

import java.time.Instant
import scala.concurrent.duration._

class TraceParserLive(tracing: Tracing, spanStore: ProcessStore) extends TraceParser {
  import tracing._

  override def parse(
      rawTrace: RawTrace,
      operationsName: String): Stream[DeepTraceError.RawTraceMappingError, TraceParsingState] = {
    def pushRoot(ref: Ref[Seq[Span]], span: Span) =
      ref.update(_ :+ span)

    def updateParentChildMap(ref: Ref[ParentToChild], span: Span) =
      ZIO.foreachDiscard(span.references.filter(_.refType.isChildOf))(r => ref.update(_.append(r.spanId, span)))

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

      _ <-
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
          .repeatWhileZIO(_ => rootRefs.get.map(_.exists(_.operationName != operationsName)))
      roots <- rootRefs.get
      parsed <- ZIO.foreach(roots)(r => fromSpan(r, parentToChildMap, r.getStartTime.toInstant))
    } yield parsed

    val traced = span("parse_single_trace")(parseOne)

    ZStream.fromZIO(traced).flatMap(chunk => ZStream.fromIterable(chunk))
  }

  def fromSpan(
      span: Span,
      parentToChildMap: ParentToChild,
      parentStart: Instant): IO[RawTraceMappingError, TraceParsingState] =
    for {
      parsedChildren <- ZIO.foreach(parentToChildMap.getOrElse(span.spanId, Seq.empty)) { child =>
        fromSpan(child, parentToChildMap, span.getStartTime.toInstant)
      }
      childInstances = parsedChildren.flatMap(_.instances)
      (concurrent, flat) = reduceConcurrent(
        parsedChildren.map(c => ParsedProcess(c.process, c.current))
      )
      reduced = concurrent ++ flat
      (gapsInstances, children) = reduceSequential(span, span.getDuration.asScala, reduced)

      start = span.getStartTime.toInstant.minus(parentStart)
      resultProcess = ParallelProcess(
        service = span.getProcess.serviceName,
        operation = span.operationName,
        children = children
      )
      resultInstant = ProcessInstance.Single(
        resultProcess.id,
        start.toDuration,
        span.getDuration.asScala
      )
    } yield TraceParsingState(
      resultProcess,
      childInstances ++ concurrent.map(_.instance) ++ gapsInstances :+ resultInstant,
      current = resultInstant,
      containsErrors = parsedChildren.exists(_.containsErrors),
      exampleId = span.requestId
    )

  def reduceConcurrent(
      processes: Seq[ParsedProcess[ParallelProcess]]): (Seq[ParsedProcess[ConcurrentProcess]], Seq[ParsedProcess[ParallelProcess]]) = {
    val grouped = processes.groupBy(_.process.id).values.groupBy(_.size > 1)

    val concurrent = grouped.getOrElse(true, Iterable.empty).map { group =>
      val processes = group.map(_.process)
      val spans = group.map(_.instance)
      val concurrentProcess = ConcurrentProcess(of = processes.head)
      val minStart = spans.map(_.start).min
      val maxEnd = spans.map(s => s.start.plus(s.duration)).max
      val instance = ProcessInstance.Concurrent(
        processId = concurrentProcess.id,
        start = minStart,
        duration = maxEnd.minus(minStart),
        count = group.size
      )
      ParsedProcess(concurrentProcess, instance)
    }

    val flat = grouped.getOrElse(false, Iterable.empty).flatten

    concurrent.toSeq -> flat.toSeq
  }

  def reduceSequential(
      parent: Span,
      parentDuration: Duration,
      processes: Seq[ParsedProcess[Process]]): (Seq[ProcessInstance.Single], Seq[SequentialProcess]) = {
    val pairs = processes.map { curr =>
      val startGap = gap(parent, path = Seq.empty, next = Some(curr.process))
      val startGapInstance = ProcessInstance.Single(startGap.id, start = 0.nanos, duration = curr.instance.start)

      val endGap = gap(parent, path = Seq(startGap, curr.process), next = None)
      val endGapInstance = ProcessInstance.Single(
        endGap.id,
        start = curr.instance.start + curr.instance.duration,
        duration = parentDuration - (curr.instance.start + curr.instance.duration)
      )

      Seq(
        startGapInstance,
        endGapInstance
      ) -> Process.SequentialProcess(
        Seq(startGap, curr.process, endGap)
      )
    }
    if (pairs.isEmpty) {
      Seq.empty -> Seq.empty
    } else {
      pairs.map(_._1).reduce(_ ++ _) -> pairs.sortBy(_._1.head.duration).map(_._2)
    }

  }
}

object TraceParserLive {
  type RawSpanId = ByteString
  type ParentToChild = Map[RawSpanId, Seq[Span]]

  private def gap(parent: Span, path: Seq[Process], next: Option[Process]): Gap = {
    val builder = new StringBuilder()
    builder.append("gap")
    builder.append(parent.processId)
    builder.append(parent.operationName)
    next match {
      case Some(value) => builder.append(value.id)
      case None => builder.append("last")
    }
    path.foreach(p => builder.append(p.id.hash))
    Gap(ProcessId(MD5.hash(builder.toString())))
  }

  case class ParsedProcess[+T <: Process](process: T, instance: ProcessInstance)

  val layer: URLayer[ProcessStore with Tracing, TraceParser] = ZLayer {
    for {
      tracing <- ZIO.service[Tracing]
      processStore <- ZIO.service[ProcessStore]
    } yield new TraceParserLive(tracing, processStore)
  }
}
