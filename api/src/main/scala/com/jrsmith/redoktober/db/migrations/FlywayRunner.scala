package com.jrsmith.redoktober.db.migrations

import com.typesafe.config.ConfigFactory

object FlywayRunner {
  def run(): Unit = {
    val config = ConfigFactory.load()
    FlywayBuilder(config).migrate()
  }
}