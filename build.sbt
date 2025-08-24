Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization := "com.alexitc.geminilive4s",
    homepage := Some(url("https://github.com/AlexITC/geminilive4s")),
    licenses := Seq(
      "MIT" -> url("https://opensource.org/license/mit")
    ),
    description := "Gemini Live API for Scala",
    startYear := Some(2025),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/AlexITC/geminilive4s"),
        "scm:git:git@github.com:AlexITC/geminilive4s.git"
      )
    ),
    developers := List(
      Developer(
        "AlexITC",
        "Alexis Hernandez",
        "alexis@alexitc.com",
        url("https://alexitc.com")
      )
    ),
    scalaVersion := "3.7.2",
    versionScheme := Some("early-semver")
  )
)

lazy val audio = project
  .in(file("audio"))
  .settings(
    name := "audio",
    libraryDependencies ++= Seq(
      "com.google.genai" % "google-genai" % "1.14.0",
      "co.fs2" %% "fs2-core" % "3.12.0",
      "co.fs2" %% "fs2-io" % "3.12.0"
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    audio
  )
  .settings(
    name := "geminilive4s",
    publish := {},
    publishLocal := {},
    publish / skip := true
  )
