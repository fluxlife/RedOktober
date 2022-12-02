package com.jrsmith.redoktober

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.jrsmith.redoktober.config.AppConfig
import com.jrsmith.redoktober.db.migrations.FlywayRunner
import com.jrsmith.redoktober.db.repositories.ImagesRepo
import com.jrsmith.redoktober.services.{ImageServiceImpl, ObjectDetectionServiceImpl}


object App {

  def startApi( system: ActorSystem ): Unit = {
    implicit val _system = system
    implicit val ec = system.dispatcher

    val imageRepo = new ImagesRepo(AppConfig.DbTransactor)

    val imageService = new ImageServiceImpl(imageRepo, ObjectDetectionServiceImpl)

    val imageRoutes = new ImageRoutes(imageService)

    val server = Http()
      .newServerAt(AppConfig.ApiConfig.host, AppConfig.ApiConfig.port)
      .bindFlow(imageRoutes.routes)

    server.failed.foreach { ex =>
      system.log.error(ex, "Error binding.")
    }
  }

  def main(args: Array[String]): Unit = {
    // Running migrations at startup. See README.md for more info.
    FlywayRunner.run()
    startApi( AppConfig.DefaultActorSystem )
  }
}
