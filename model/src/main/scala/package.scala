package io.github.mcsim4s

package object dt {
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
}
