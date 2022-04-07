package io.github.mcsim4s.dt.engine

import com.google.protobuf.ByteString
import io.github.mcsim4s.dt.engine.TraceParser.TraceParser
import io.github.mcsim4s.dt.engine.live.TraceParserLive
import io.github.mcsim4s.dt.engine.live.store.LiveSpanStore
import io.github.mcsim4s.dt.model.RawTrace
import io.jaegertracing.api_v2.model.{Span, SpanRef, SpanRefType}
import zio.ZLayer
import zio.test.Assertion.{equalTo, hasField, hasSize}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, assert, assertM}
import zio.magic._

object ClusterStatsParserSpec extends DefaultRunnableSpec {

  val singleSpanTrace: RawTrace = RawTrace(
    Seq(
      Span(
        traceId = ByteString.copyFromUtf8("singe span trace"),
        spanId = ByteString.copyFromUtf8("the only span"),
        operationName = "single operation"
      )
    )
  )

  val singleChildTrace: RawTrace = {
    val id = ByteString.copyFromUtf8("singe child trace")
    RawTrace(
      Seq(
        Span(
          traceId = id,
          spanId = ByteString.copyFromUtf8("root"),
          operationName = "single child operation"
        ),
        Span(
          traceId = id,
          spanId = ByteString.copyFromUtf8("child"),
          operationName = "child operation",
          references = Seq(
            SpanRef(
              traceId = id,
              spanId = ByteString.copyFromUtf8("root"),
              refType = SpanRefType.CHILD_OF
            )
          )
        )
      )
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Trace conversions spec")(
      testM("convert singe span raw trace to trace") {
        assertM(
          TraceParser
            .parse(singleSpanTrace)
//            .tap(trace => zio.console.putStrLn(trace.toString))
        )(hasField("operation", _.operation, equalTo("single operation")))
      },
      testM("Convert one child raw trace to trace") {
        for {
          process <-
            TraceParser
              .parse(singleChildTrace)
              .tap(trace => zio.console.putStrLn(trace.toString))
        } yield assert(process.children)(hasSize(equalTo(1)))
      }
    ).provideCustomLayerShared(
      ZLayer.wireSome[TestEnvironment, TestEnvironment with TraceParser](
        LiveSpanStore.layer,
        TraceParserLive.layer
      )
    )

}
