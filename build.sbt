ThisBuild / version       := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion  := "3.3.1"

lazy val root = (project in file("."))
  .settings(
      name := "arschloch",

      // Tests
      libraryDependencies ++= Seq(
          "org.scalatest" %% "scalatest" % "3.2.18" % Test
      ),

      // Scoverage aktivieren
      coverageEnabled := true,

      // Packages/Files, die Scoverage ignorieren soll.
      // Mehrere Einträge per Semikolon in der Regex:
      coverageExcludedPackages := "de\\.htwg\\.arschloch\\.ArschlochGame;htwg\\.de\\.TUI\\..*",
      coverageExcludedFiles    := ".*TUI\\.scala" // Exkludiere pure List-Ident-Lookups in ArschlochGame


)
