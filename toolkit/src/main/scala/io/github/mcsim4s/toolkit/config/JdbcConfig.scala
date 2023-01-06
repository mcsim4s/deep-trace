package io.github.mcsim4s.toolkit.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class JdbcConfig(
    name: String,
    driver: String,
    url: String,
    user: String,
    password: String,
    readOnly: Boolean = false,
    props: Map[String, String] = Map.empty)

object JdbcConfig {
  implicit val reader: ConfigReader[JdbcConfig] = deriveReader[JdbcConfig]
}
