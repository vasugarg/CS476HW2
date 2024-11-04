import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "CS476HW2"
  )

libraryDependencies ++= {
  val logbackVersion = "1.5.6"

  Seq(
    "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )
}
