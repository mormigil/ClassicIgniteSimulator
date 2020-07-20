ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "2.11.12"
ThisBuild / organization := "willis"

lazy val circeVersion    = "0.10.0"
//
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
//
mainClass in (Compile, run) := Some("com.willis.simulator.IgniteTest")
//
lazy val simulator = (project in file("."))
  .settings(
    name := "mage-simulator",
    assemblyJarName := "mage-simulator.jar",
    test in assembly := {},
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "3.2.0",
      "org.apache.spark" %% "spark-core" % "2.4.2" % "provided",
      "org.apache.spark" %% "spark-sql" % "2.4.2" % "provided",
      "com.lihaoyi" %% "requests" % "0.1.7",
      "com.typesafe" % "config" % "1.3.4",

      //JSON
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      //JSON/Serialization
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      //Test
      "org.scalatest" %% "scalatest" % "3.1.0" % "test",
      "org.scalamock" %% "scalamock" % "4.4.0" % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test"
    )
  )