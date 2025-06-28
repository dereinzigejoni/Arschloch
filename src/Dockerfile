# ───────────────────────────────────────────────────────────────────────────────
# 1. Build-Stage: SBT & Assembly
# ───────────────────────────────────────────────────────────────────────────────
FROM hseeberger/scala-sbt:11.0.16_1.8.24_2.13.10 AS builder

# Arbeitsverzeichnis
WORKDIR /app

# Nur SBT-Metadaten kopieren, damit Docker-Cache wirkt
COPY build.sbt project/ ./ 
COPY project/plugins.sbt project/

# Cache warm-up (lädt Plugins & Dependencies)
RUN sbt sbtVersion

# Restlichen Code kopieren
COPY src/ ./src/

# Erzeuge das Fat-Jar via sbt-assembly
RUN sbt clean assembly

# ───────────────────────────────────────────────────────────────────────────────
# 2. Runtime-Stage: OpenJDK + JavaFX
# ───────────────────────────────────────────────────────────────────────────────
FROM openjdk:17-slim

LABEL maintainer="Dein Name <deine.email@adresse>"

WORKDIR /app

# Installiere OpenJFX für ScalaFX-GUI
RUN apt-get update \
 && apt-get install -y --no-install-recommends openjfx \
 && rm -rf /var/lib/apt/lists/*

# Kopiere das erstellte Assembly-Jar
COPY --from=builder /app/target/scala-2.13/blackjack-assembly-*.jar ./blackjack.jar

# Optional: Speicher- und Netzwerk-Optimierungen
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Standard-Start: TUI. Für GUI weiter unten X11-Forwarding nutzen.
ENTRYPOINT ["java","-jar","blackjack.jar"]
