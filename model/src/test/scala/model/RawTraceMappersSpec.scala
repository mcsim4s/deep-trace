package io.github.mcsim4s.dt
package model

import model.mappers.RawTraceMappers

import com.google.protobuf.ByteString
import io.jaegertracing.api_v2.model.{Span, SpanRef, SpanRefType}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test._

object RawTraceMappersSpec extends DefaultRunnableSpec {

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
          RawTraceMappers
            .fromRaw(singleSpanTrace)
//            .tap(trace => zio.console.putStrLn(trace.toString))
        )(hasField("name", _.name, equalTo("single operation")))
      },
      testM("Convert one child raw trace to trace") {
        for {
          process <-
            RawTraceMappers
              .fromRaw(singleChildTrace)
              .tap(trace => zio.console.putStrLn(trace.toString))
        } yield assert(process.children)(hasSize(equalTo(1)))
      }
    )

}
