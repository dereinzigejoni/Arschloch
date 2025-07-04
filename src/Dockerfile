FROM hseeberger/scala-sbt:11.0.16_1.8.24_2.13.10 AS builder
WORKDIR /app
COPY build.sbt project/ ./ 
COPY project/plugins.sbt project/
RUN sbt sbtVersion
COPY src/ ./src/
RUN sbt clean assembly
FROM openjdk:17-slim
LABEL maintainer="Dein Name <deine.email@adresse>"
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends openjfx \
 && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/scala-2.13/blackjack-assembly-*.jar ./blackjack.jar
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java","-jar","blackjack.jar"]
