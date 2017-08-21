name := "AkkaStreamsJsonTransform"

version := "1.0"

scalaVersion := "2.12.3"

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16"
)

libraryDependencies ++= akkaDependencies
