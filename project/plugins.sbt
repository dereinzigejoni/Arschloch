// project/plugins.sbt

// Damit sbt auch auf das offizielle Plugin-Repo zugreift:
resolvers ++= Seq(
  "sbt-plugin-releases" at "https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

// Code-Coverage via scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage"  % "2.3.1")
// Upload der Coverage-Ergebnisse nach Coveralls
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.1")
