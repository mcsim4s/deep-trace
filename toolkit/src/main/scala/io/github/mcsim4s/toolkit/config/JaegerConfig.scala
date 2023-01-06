package io.github.mcsim4s.toolkit.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class JaegerConfig(url: String, serviceName: String)

object JaegerConfig {
  implicit val reader: ConfigReader[JaegerConfig] = deriveReader[JaegerConfig]
}
