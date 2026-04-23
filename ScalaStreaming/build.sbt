name := "ScalaStreaming"

organization := "scala.streaming"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.16"

Compile / unmanagedSourceDirectories += baseDirectory.value / "app"
Compile / unmanagedResourceDirectories += baseDirectory.value / "conf"
Compile / run / fork := true
Compile / run / envVars ++= {
  val envFile = baseDirectory.value / ".env"
  if (envFile.exists()) {
    scala.io.Source.fromFile(envFile).getLines()
      .filterNot(line => line.trim.startsWith("#") || line.trim.isEmpty)
      .flatMap { line =>
        val parts = line.split("=", 2)
        if (parts.length == 2)
          Some(parts(0).trim -> parts(1).trim.stripPrefix("\"").stripSuffix("\""))
        else None
      }
      .toMap
  } else Map.empty
}
Compile / run / javaOptions ++= Seq(
  "-Dlogback.configurationFile=conf/logback.xml",
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-exports=java.base/sun.security.action=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED"
)

Global / logLevel := Level.Info

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % "3.5.1",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.19.1",
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",
  "com.github.pureconfig" %% "pureconfig-generic" % "0.17.8",
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "org.mongodb" % "mongodb-driver-sync" % "5.1.4",
  "io.github.cdimascio" % "dotenv-java" % "3.0.2"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _*)             => MergeStrategy.discard
  case "reference.conf"                     => MergeStrategy.concat
  case _                                    => MergeStrategy.first
}

assembly / mainClass := Some("app.streaming.OrderSparkStreamingJob")
