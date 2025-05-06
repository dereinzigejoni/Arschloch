// build.sbt

name := "Blackjack"

version := "0.1.0"

scalaVersion := "2.13.10"

// ScalaFX braucht seinen eigenen Resolver
resolvers += "scalafx-releases" at "https://repo.scala-lang.org/scalafx/releases/"

libraryDependencies ++= Seq(
  "org.scalatest"   %% "scalatest"  % "3.2.19" % Test,
  "org.scalatest"   %% "scalatest-wordspec" % "3.2.18" % Test,
  // hier die ScalaFX-Dependency
  "org.scalafx"     %% "scalafx"    % "22.0.0-R33"
)


