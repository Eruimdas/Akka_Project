name := "akka_tryout"

version := "0.1"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % Test
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % Test
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23" % Test
)

libraryDependencies += "io.spray" %% "spray-json" % "1.3.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.4"

libraryDependencies += "org.apache.kafka" %% "kafka" % "2.3.0"

