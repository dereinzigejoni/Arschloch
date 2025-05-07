error id: 
file://<WORKSPACE>/build.sbt
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 511
uri: file://<WORKSPACE>/build.sbt
text:
```scala
// build.sbt
ThisBuild / scalaVersion := "2.13.10"

// 1) Plugins aktivieren
enablePlugins(
  scoverage.ScoverageSbtPlugin,
  org.scoverage.coveralls.CoverallsPlugin
)

// 2) Keys importieren
import scoverage.ScoverageKeys._                // coverageEnabled, coverageMinimum, …
import org.scoverage.coveralls.Imports.CoverallsKeys._  // coverallsToken, coverallsBranch, …

// 3) Deine normalen Abhängigkeiten
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test

// ——— scoverage konfigurier@@en ———
coverageEnabled       := true    // Coverage an
//coverageMinimum       := 80      // Minimum 80 %
coverageFailOnMinimum := true    // Build-Fail, wenn < 80 %

// ——— Coveralls konfigurieren ———
// hol dir das Token aus der ENV
coverallsToken        := sys.env.get("COVERALLS_REPO_TOKEN")
// Branch-Name (GitHub Actions setzt GITHUB_REF_NAME automatisch)
coverallsBranch       := sys.env.get("GITHUB_REF_NAME")
// Service-Name (optional, der Plugin erkennt GitHub Actions aber auch automatisch)
// Typ passt genau, weil coverallsServiceName: SettingKey[Option[String]]
coverallsServiceName  := Some("github-actions")

```


#### Short summary: 

empty definition using pc, found symbol in pc: 