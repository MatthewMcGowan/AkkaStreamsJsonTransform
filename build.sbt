name := "AkkaStreamsJsonTransform"

version := "1.0"

scalaVersion := "2.12.3"

val configDependencies = Seq(
  "com.typesafe" % "config" % "1.3.1"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16"
)

val jsonDependencies = Seq(
  "com.typesafe.play" %% "play-json" % "2.6.3"
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "net.manub" %% "scalatest-embedded-kafka" % "0.15.1" % "test"
)

libraryDependencies ++=
  configDependencies ++
    akkaDependencies ++
    testDependencies ++
    jsonDependencies
