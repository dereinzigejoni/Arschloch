// build.sbt
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "de.htwg.blackjack"

lazy val root = (project in file("."))
  // Scoverage aktivieren
  .settings(
    name := "Blackjack",

    // Dependencies
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "22.0.0-R33",
      "org.scalatest"          %% "scalatest"   % "3.2.19" % Test,
      "com.google.inject" % "guice" % "7.0.0",
      "org.scalamock" %% "scalamock" % "6.0.0" % Test


    ),

    // Scoverage‐Optionen
    coverageEnabled := true,
coverageHighlighting := true,   // Syntax-Hervorhebung im Bericht

    // Compiler-Options

    // Ressourcen
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources"
  )
