ThisBuild / organization     := "t-academy"
ThisBuild / organizationName := "T-Bank"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / fork             := false

ThisBuild / scalaVersion                                  := "2.13.18"
ThisBuild / scalafixDependencies += "org.typelevel"       %% "typelevel-scalafix" % "0.5.0"
ThisBuild / scalafixDependencies += "com.github.vovapolu" %% "scaluzzi"           % "0.1.23"
ThisBuild / semanticdbEnabled                             := true
ThisBuild / semanticdbVersion                             := "4.15.2"
ThisBuild / scalacOptions ++= List(
  "-Ymacro-annotations",
)

val bot = project.settings(
  libraryDependencies ++= Dependencies.allDeps,
  libraryDependencies ++= List(
    "org.augustjune" %% "canoe" % "0.6.0"
  ),
  Test / scalafixConfig := Some((ThisBuild / baseDirectory).value / ".scalafix-test.conf")
)
val scrapper = project.settings(libraryDependencies ++= Dependencies.allDeps)
val ai       = project.settings(name := "ai-agent", libraryDependencies ++= Dependencies.allDeps)
