name := """SmartOrder"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

// Set engine type to NODE (see also SBT_OPTS)
// Checks for package.json in project's base directory then causes npm to run
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

fork in run := false

