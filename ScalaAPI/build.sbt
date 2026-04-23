name := """ScalaAPI"""
organization := "scalapark.api"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.9.2"
libraryDependencies += "com.github.pureconfig" %% "pureconfig-core" % "0.17.8"
