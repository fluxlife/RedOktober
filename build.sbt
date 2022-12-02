ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val Http4sVersion = "0.23.10"
val GoogleCloudVision = "3.4.0"
val CirceVersion = "0.14.1"
val DoobieVersion = "1.0.0-RC1"
val AkkaVersion = "2.7.0"
val LogbackVersion = "1.4.5"
val AkkaHttpVersion = "10.4.0"
val AkkaHttpJsonVersion = "1.39.2"

val apiDeps = Seq(
  // HTTP
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  // Image / Object detection
  "com.google.cloud" % "google-cloud-vision" % GoogleCloudVision,
  // JSON
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  // Database ORM
  "org.tpolecat" %% "doobie-core"      % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari"    % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres"  % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres-circe" % DoobieVersion,
  "org.tpolecat" %% "doobie-scalatest" % DoobieVersion % Test,
  // Akka
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % AkkaHttpJsonVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test,
  // DB Driver
  "org.postgresql" % "postgresql" % "42.2.16",
  // DB Migrations
  "org.flywaydb" % "flyway-core" % "9.8.1",
  // DB Connection Pooling
  "com.zaxxer" % "HikariCP" % "5.0.1",
  // Config
  "com.github.pureconfig" %% "pureconfig" % "0.17.2",
  // Logging
  "ch.qos.logback"        %  "logback-classic"      % LogbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  // Testing
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.14.0" % Test
)

lazy val api = (project in file("api"))
  .settings(
    name := "RedOktober-API",
    libraryDependencies ++= apiDeps,
    Test / javaOptions += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.test.conf",
    assembly / mainClass := Some("package com.jrsmith.redoktober.App"),
    assembly / assemblyJarName := "app.jar",
    ThisBuild / assemblyMergeStrategy := {
      case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
      case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
      case "application.conf" => MergeStrategy.concat
      case x =>
        MergeStrategy.last
//        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
//        oldStrategy(x)
    }
  )
