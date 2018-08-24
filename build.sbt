name := "little-json"
version := "0.6.0"
organization := "losizm"

scalaVersion := "2.12.6"
scalacOptions ++= Seq("-deprecation", "-feature", "-Xcheckinit")

libraryDependencies ++= Seq(
  "org.glassfish" % "javax.json" % "1.1.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
