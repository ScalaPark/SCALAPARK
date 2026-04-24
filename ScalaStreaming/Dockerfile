FROM eclipse-temurin:17 AS builder
RUN apt-get update && apt-get install -y curl && \
    curl -fL https://github.com/sbt/sbt/releases/download/v1.10.11/sbt-1.10.11.tgz | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt
WORKDIR /app
RUN mkdir -p /root/.sbt && echo '-java-home /opt/java/openjdk' > /root/.sbt/sbtopts
COPY build.sbt .
COPY project/ project/
RUN sbt update
COPY . .
RUN echo '-java-home /opt/java/openjdk' > .sbtopts && sbt assembly

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/scala-2.13/ScalaStreaming-assembly-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", \
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-exports=java.base/sun.security.action=ALL-UNNAMED", \
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED", \
  "-jar", "app.jar"]
