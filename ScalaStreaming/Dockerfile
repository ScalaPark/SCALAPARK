FROM eclipse-temurin:17 AS builder
RUN apt-get update && apt-get install -y curl && \
    curl -fL https://github.com/sbt/sbt/releases/download/v1.10.11/sbt-1.10.11.tgz | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt
WORKDIR /app
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
