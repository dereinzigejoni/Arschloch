// build.sbt
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "de.htwg.blackjack"

lazy val root = (project in file("."))
  .settings(
    name := "Blackjack"
  )


    // Dependencies
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "22.0.0-R33",
      "org.scalatest"          %% "scalatest"   % "3.2.19" % Test,
      "com.google.inject" % "guice" % "7.0.0",
      "org.scalamock" %% "scalamock" % "6.0.0" % Test,
      // XML‐Support
      "org.scala-lang.modules" %% "scala-xml"      % "2.3.0",

      // JSON‐Serialisierung mit uPickle
      "com.lihaoyi"        %% "upickle"           % "4.0.2",

      // OS‐Lib zum einfachen File‐Handling in JsonFileIO
      "com.lihaoyi"        %% "os-lib"            % "0.11.2",
    )
  enablePlugins(ScoverageSbtPlugin)
  enablePlugins(CoverallsPlugin)


