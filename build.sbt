name := "Barb"

version := "0.1"

scalaVersion := "2.11.5"

resolvers += "ML Repository" at "http://temerev.com:8080/archiva/repository/internal/"

libraryDependencies ++= Seq(
  "org.quickfixj" % "quickfixj-all" % "1.5.3",
  "org.apache.mina" % "mina-core" % "1.1.7",
  "org.apache.mina" % "mina-filter-ssl" % "1.1.7",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "com.miriamlaurel" %% "fxcore" % "1.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)