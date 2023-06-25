package io.github.mcsim4s.dt

import com.google.protobuf.timestamp.Timestamp
import io.jaegertracing.api_v2.model.Span

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

package object model {

  implicit class RichMap[K, V](val map: Map[K, V]) {

    def putIfAbsent(k: K, v: V): Map[K, V] = {
      if (map.contains(k)) {
        map
      } else {
        map + ((k, v))
      }
    }
  }

  implicit class MapCollector[K, V](val map: Map[K, Seq[V]]) {

    def append(k: K, v: V): Map[K, Seq[V]] = {
      val withEmpty = map.putIfAbsent(k, Seq.empty)
      withEmpty + ((k, withEmpty(k) :+ v))
    }
  }

  implicit class RichTimestamp(val timeStamp: Timestamp) {
    def toInstant: Instant = Instant.ofEpochSecond(timeStamp.seconds, timeStamp.nanos)

    def plus(duration: com.google.protobuf.duration.Duration): Timestamp = {
      Timestamp.of(timeStamp.seconds + duration.seconds, timeStamp.nanos + duration.nanos)
    }

    def minus(other: Timestamp): com.google.protobuf.duration.Duration = {
      val seconds = other.seconds - timeStamp.seconds
      val nanos = other.nanos - timeStamp.nanos

      if (seconds * nanos >= 0) {
        com.google.protobuf.duration.Duration.of(seconds, nanos)
      } else if (seconds > 0) {
        val ext = math.ceil(nanos / -1000d).toInt
        if (seconds - ext > 0) {
          com.google.protobuf.duration.Duration.of(seconds - ext, nanos + ext * 1000)
        } else throw new IllegalArgumentException("Too complicated")
      } else throw new IllegalArgumentException("Too complicated")
    }
  }

  implicit class RichInstant(val instant: Instant) {

    def minus(other: Instant): Instant = {
      instant.minusSeconds(other.getEpochSecond).minusNanos(other.getNano)
    }

    def toDuration: Duration = {
      Duration(instant.getEpochSecond, TimeUnit.SECONDS).plus(Duration(instant.getNano, TimeUnit.NANOSECONDS))
    }

    def toTimeStamp: Timestamp = Timestamp.of(instant.getEpochSecond, instant.getNano)
  }

  implicit class RichDuration(val duration: com.google.protobuf.duration.Duration) {

    def asScala: Duration = {
      Duration(duration.seconds, TimeUnit.SECONDS).plus(Duration(duration.nanos, TimeUnit.NANOSECONDS))
    }
  }

  implicit class RichJaegerSpan(val span: Span) {
    lazy val requestId: String = span.traceId.toByteArray.map(String.format("%02x", _)).mkString
  }
}
