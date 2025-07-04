# Dockerfile
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.2_3.5.0

WORKDIR /app
COPY build.sbt            ./
COPY project/             ./project/
COPY src/                 ./src/

# Cache Dependencies & Kompilierung
RUN sbt update compile

# Starte im Container per Shell-Form, damit die Anführungszeichen greifen
CMD sbt -no-colors "runMain de.htwg.blackjack.main.TuiMain"
