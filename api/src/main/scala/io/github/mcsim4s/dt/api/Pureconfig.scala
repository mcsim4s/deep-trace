package io.github.mcsim4s.dt.api

import pureconfig.{ConfigReader, ConfigSource}
import zio._

import scala.reflect.ClassTag

object Pureconfig {

  def load[A: ClassTag: ConfigReader: Tag](namespace: String): ZLayer[Any, Throwable, A] = ZLayer {
    ZIO.from {
      ConfigSource.default.at(namespace).loadOrThrow[A]
    }
  }
}
