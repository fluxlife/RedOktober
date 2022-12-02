package com.jrsmith.redoktober.db.migrations

import com.typesafe.config.Config
import org.flywaydb.core.Flyway

import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

object FlywayBuilder {
  // The namespace in a conf file to pull config values from
  val ConfigNamespace = "flyway"

  /**
   * Flyway is a native Java migration library and does not now how to
   * utilize Typesafe config. Creating an implicit conversion to a
   * Properties file which it does know how to use.
   *
   * @param config Typesafe Config object
   * @return Properties
   */
  implicit def configToProperties(config: Config): Properties = {
    val properties = new Properties()
    config
      .getConfig(ConfigNamespace)
      .entrySet
      .asScala
      .foreach { entry =>
        val configKey = s"$ConfigNamespace.${entry.getKey}"
        properties.setProperty(configKey, config.getString(configKey))
      }
    properties
  }

  def apply(config: Config): Flyway = {
    Flyway
      .configure()
      .configuration(config)
      .load()
  }
}