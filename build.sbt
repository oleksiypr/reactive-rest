name := "reactive-rest"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.9",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)



