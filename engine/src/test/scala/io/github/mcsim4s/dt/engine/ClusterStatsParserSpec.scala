package io.github.mcsim4s.dt.engine

import com.google.protobuf.ByteString
import io.github.mcsim4s.dt.engine.live.TraceParserLive
import io.github.mcsim4s.dt.engine.live.store.LiveProcessStore
import io.github.mcsim4s.dt.model.RawTrace
import io.jaegertracing.api_v2.model.{Span, SpanRef, SpanRefType}
import zio.test.Assertion.{equalTo, hasField, hasSize, isSome}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assert}

object ClusterStatsParserSpec extends ZIOSpecDefault {

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

  override def spec: Spec[TestEnvironment, Any] =
    suite("Trace conversions spec")(
      test("convert singe span raw trace to trace") {
        TraceParser
          .parse(singleSpanTrace, "single operation")
          .runHead
          .map(assert(_)(isSome(hasField("operation", _.process.operation, equalTo("single operation")))))
      },
      test("Convert one child raw trace to trace") {
        TraceParser
          .parse(singleChildTrace, "single child operation")
          .runHead
          .map(assert(_)(isSome(hasField("children", _.process.children, hasSize(equalTo(1))))))
      }
    ).provideShared(
      LiveProcessStore.layer,
      TraceParserLive.layer
    )

}
