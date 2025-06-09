import Dependencies.{dependencies, testDependencies}

val scala3Version = "3.7.1"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .settings(
    name := "zionomicon-exercise-solutions",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= dependencies,
    libraryDependencies ++= testDependencies.map(_ % Test)
  )
