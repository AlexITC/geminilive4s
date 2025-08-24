Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization := "com.alexitc.geminilive4s",
    homepage := Some(url("https://github.com/AlexITC/geminilive4s")),
    licenses := Seq(
      "MIT" -> url("https://github.com/AlexITC/geminilive4s/blob/main/LICENSE")
    ),
    developers := List(
      Developer(
        "alexitc",
        "Alexis Hernandez",
        "alexis@alexitc.com",
        url("https://alexitc.com")
      )
    ),
    scalaVersion := "3.7.2"
  )
)

lazy val audio = project
  .in(file("audio"))
  .settings(
    moduleName := "audio",
    libraryDependencies ++= Seq(
      "com.google.genai" % "google-genai" % "1.10.0",
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
