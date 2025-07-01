
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "de.htwg.blackjack"

lazy val root = (project in file("."))
  .enablePlugins(ScoverageSbtPlugin, CoverallsPlugin) // Plugins aktivieren
  .settings(
    name := "Blackjack",

    // Coverage aktivieren
    coverageEnabled      := true,
    coverageHighlighting := true,

    // Dependencies
    libraryDependencies ++= Seq(
      "org.scalafx"        %% "scalafx"       % "22.0.0-R33",
      "org.scalatest"      %% "scalatest"     % "3.2.19"  % Test,
      "com.google.inject"  %  "guice"         % "7.0.0",
      "org.scalamock"      %% "scalamock"     % "6.0.0"   % Test,
      "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
      "com.lihaoyi"        %% "upickle"       % "4.0.2",
      "com.lihaoyi"        %% "os-lib"        % "0.11.2"
    )
  )
