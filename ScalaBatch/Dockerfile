# --- Stage 1: builder ---
# Uses a pre-built sbt + JDK image. We split the COPY into two layers so dependency
# resolution (sbt update) is cached independently from source changes.
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.9_2.13.18 AS builder
WORKDIR /app

# Build definition first — cached as long as deps don't change.
COPY build.sbt ./
COPY project/ project/
RUN sbt update

# Sources and resources — changes here don't invalidate the dep cache above.
COPY app/ app/
COPY conf/ conf/
RUN sbt assembly

# --- Stage 2: runtime ---
# JRE only (no JDK, no sbt, no build cache). Alpine keeps the image minimal.
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /app/target/scala-2.13/ScalaBatch-assembly-0.1.0-SNAPSHOT.jar /app/scalabatch.jar

# Drop root. Spark doesn't need elevated privileges to run the batch job.
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app
USER appuser

# The four --add-exports/--add-opens flags are required by Spark 3.5 on JDK 17.
ENTRYPOINT ["java", \
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-exports=java.base/sun.security.action=ALL-UNNAMED", \
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED", \
  "-jar", "/app/scalabatch.jar"]
