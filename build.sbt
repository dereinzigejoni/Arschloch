// build.sbt
ThisBuild / scalaVersion := "3.5.0"
name := "blackjack"
version := "0.1.0"

enablePlugins(scoverage.ScoverageSbtPlugin)
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
coverageEnabled := true
//coverageMinimumBranchTotal:= 80
//coverageFalOnMinimum := true



