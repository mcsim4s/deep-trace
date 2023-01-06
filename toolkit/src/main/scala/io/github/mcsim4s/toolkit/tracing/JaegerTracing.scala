package io.github.mcsim4s.toolkit.tracing

import io.github.mcsim4s.toolkit.config.{JaegerConfig, Pureconfig}
import io.opentelemetry.api.common._
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.jaeger._
import io.opentelemetry.sdk._
import io.opentelemetry.sdk.resources._
import io.opentelemetry.sdk.trace._
import io.opentelemetry.sdk.trace.`export`._
import io.opentelemetry.semconv.resource.attributes._
import zio._
import zio.telemetry.opentelemetry.Tracing

object JaegerTracing {

  def live: TaskLayer[Tracing] =
    ZLayer {
      for {
        config <- Pureconfig.load[JaegerConfig]("jaeger")
        tracer <- makeTracer(config)
      } yield tracer
    } >>> Tracing.live

  private def makeTracer(config: JaegerConfig): Task[Tracer] =
    for {
      spanExporter <- ZIO.attempt(JaegerGrpcSpanExporter.builder().setEndpoint(config.url).build())
      spanProcessor <- ZIO.succeed(SimpleSpanProcessor.create(spanExporter))
      tracerProvider <-
        ZIO.attempt(
          SdkTracerProvider
            .builder()
            .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, config.serviceName)))
            .addSpanProcessor(spanProcessor)
            .build()
        )
      openTelemetry <- ZIO.succeed(OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build())
      tracer <- ZIO.succeed(openTelemetry.getTracer(config.serviceName))
    } yield tracer
}
