FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.9_2.13.18 AS builder
WORKDIR /app
COPY build.sbt ./
COPY project/ project/
RUN sbt update
COPY . .
RUN sbt assembly

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/scala-2.13/ScalaStreaming-assembly-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", \
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-exports=java.base/sun.security.action=ALL-UNNAMED", \
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED", \
  "-jar", "app.jar"]
