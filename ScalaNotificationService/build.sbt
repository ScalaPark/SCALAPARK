name := "ScalaNotificationService"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.16"

Compile / run / fork := true

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "org.apache.kafka" % "kafka-clients" % "3.9.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
  "com.typesafe" % "config" % "1.4.3",
  "org.apache.poi" % "poi-ooxml" % "5.3.0"
)
