name := """ScalaEcommerce"""
organization := "scala.ecommerce"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.9.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.1"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1"
libraryDependencies += "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.19.1"
libraryDependencies += "com.github.pureconfig" %% "pureconfig-core" % "0.17.8"
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "scala.ecommerce.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "scala.ecommerce.binders._"
