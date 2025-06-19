ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "de.htwg.blackjack"
ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  // Scoverage aktivieren
  .enablePlugins(ScoverageSbtPlugin)
  .settings(
    name := "Blackjack",

    // Dependencies
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "22.0.0-R33",
      "org.scalatest"          %% "scalatest"   % "3.2.19" % Test,
      "com.google.inject" % "guice" % "7.0.0"


    ),

    // Scoverage‐Optionen
    coverageHighlighting := true, // Highlighting im Report

    // Compiler-Options
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xfatal-warnings"
    ),

    // Ressourcen
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources"
  )