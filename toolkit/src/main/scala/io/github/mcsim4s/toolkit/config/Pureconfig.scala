package io.github.mcsim4s.toolkit.config

import pureconfig.{ConfigReader, ConfigSource}
import zio._

import scala.reflect.ClassTag

object Pureconfig {

  def loadLayer[A: ClassTag: ConfigReader: Tag](namespace: String): ZLayer[Any, Throwable, A] = ZLayer {
    load(namespace)
  }

  def load[A: ClassTag: ConfigReader: Tag](namespace: String): ZIO[Any, Throwable, A] =
    ZIO.from {
      ConfigSource.default.at(namespace).loadOrThrow[A]
    }

}
