# Dockerfile
# 1) Basis-Image mit Scala 3.5.0, sbt 1.10.2 und Eclipse Temurin 17
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.2_3.5.0

# 2) Arbeitsverzeichnis setzen
WORKDIR /app

# 3) Kopiere Projektdateien ins Image
COPY build.sbt            ./
COPY project/             ./project/
COPY src/                 ./

# 4) Vorab Dependencies holen & kompilieren (Cache)
RUN sbt update compile

# 5) Standard-Kommando: Starte Deine TUI-Mainklasse
CMD ["sbt", "-no-colors", "runMain", "de.htwg.blackjack.main.TuiMain"]
