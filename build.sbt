// build.sbt

// ---- Projekt‐Meta ----
ThisBuild / organization := "de.htwg.blackjack"
ThisBuild / version      := "0.1.0-SNAPSHOT"
// Du hast Scala 3-Code (wildcards `import model.*`), also:
ThisBuild / scalaVersion := "3.5.0"

// Sonatype für ScalaFX und andere Bibliotheken
//resolvers += Resolver.sonatypeRepo("public")

lazy val root = (project in file("."))
  .settings(
    name := "ScalaBlackjack",

    // zwingend forken, damit JavaFX in eigenen JVM‐Prozess läuft
    Compile / fork := true,

    // Compiler‐Optionen
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8"
    ),

    // Deine Bibliotheken
    libraryDependencies ++= Seq(
      // Test‐Framework
      "org.scalatest"  %% "scalatest"  % "3.2.19" % Test,
      "org.scalactic"  %% "scalactic"  % "3.2.19" % Test,

      // ScalaFX für GUI (Version für Scala 3.x)
      "org.scalafx"    %% "scalafx"    % "22.0.0-R33"
    ),

    // ScalaTest als Test‐Framework bekanntmachen
    testFrameworks += new TestFramework("org.scalatest.tools.Framework"),

    // Scoverage‐Plugin‐Einstellungen (falls du Code‐Coverage nutzt)
    // Coverage aktivieren
    coverageEnabled      := true,
    // Mindest‐Coverage (in %)
    coverageMinimumBranchTotal      := 70,
    // Build fehlschlagen lassen, wenn Minimum nicht erreicht
    coverageFailOnMinimum:= true
  )
