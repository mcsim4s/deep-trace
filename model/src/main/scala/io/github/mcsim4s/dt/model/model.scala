package io.github.mcsim4s.dt

import com.google.protobuf.timestamp.Timestamp

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
}
