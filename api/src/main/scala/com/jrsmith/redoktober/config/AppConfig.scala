package com.jrsmith.redoktober.config

import akka.actor.ActorSystem
import cats.effect.{IO, Resource}
import com.google.auth.oauth2.GoogleCredentials
import doobie.hikari._
import doobie.implicits._
import pureconfig._
import pureconfig.generic.auto._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object HikariConfigValues{
  val Namespace = "redoktober.hikari"
}
case class HikariConfigValues(driver: String, url: String, user: String, password: String, schema: String)

object ApiConfigValues{
  val Namespace = "redoktober.api"
}
case class ApiConfig(host: String, port: Int)

object AppConfig{

  //Execution Context dedicated for database connections
  lazy val DbExecutorContext = ExecutionContext.fromExecutor( Executors.newCachedThreadPool() )

  lazy val HikariConf = ConfigSource.default.at( HikariConfigValues.Namespace ).loadOrThrow[HikariConfigValues]

  lazy val ApiConfig = ConfigSource.default.at( ApiConfigValues.Namespace ).loadOrThrow[ ApiConfig ]

  lazy val DefaultActorSystem = ActorSystem("redoktober")

  //Google Credentials needed for _manually_ loading in creds to Google Clients.
  lazy val GoogleCreds = GoogleCredentials.fromStream(
    getClass.getResourceAsStream("/redoktober-google-vision.json")
  )

  // Doobie DbTransactor utilizing HikariCP. Executes Doobie queries.
  lazy val DbTransactor: Resource[IO, HikariTransactor[IO]] =
    HikariTransactor
      .newHikariTransactor(
        HikariConf.driver,
        HikariConf.url,
        HikariConf.user,
        HikariConf.password,
        DbExecutorContext)

}
