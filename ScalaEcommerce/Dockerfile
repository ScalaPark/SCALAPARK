FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.9_2.13.18 AS builder
WORKDIR /app
COPY . .
RUN sbt stage

FROM eclipse-temurin:17-jre AS runner
WORKDIR /app
COPY --from=builder /app/target/universal/stage .
COPY dataset ./dataset
EXPOSE 9000
ENTRYPOINT ["bin/scalaecommerce"]
