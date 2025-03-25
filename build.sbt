ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1" // Empfohlene stabile Scala-Version

lazy val root = (project in file("."))
  .settings(
    name := "arschloch",

    // ScalaTest als Test-Framework hinzufügen
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.17" % Test
  )
