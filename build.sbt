// build.sbt
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "de.htwg.blackjack"

lazy val root = (project in file("."))
  // Scoverage aktivieren
  .enablePlugins(ScoverageSbtPlugin)
  .settings(
    name := "ScalaBlackjack",

    // Dependencies
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
      "org.scalatest"          %% "scalatest"   % "3.2.19" % Test
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
