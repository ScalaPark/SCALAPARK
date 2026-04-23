FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.9_2.13.18 AS runner

WORKDIR /app

# Copy build definition first to improve layer caching.
COPY build.sbt ./
COPY project ./project
RUN sbt update

COPY src ./src

CMD ["sbt", "run"]
