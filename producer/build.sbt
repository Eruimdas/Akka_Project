name := "producer"

version := "0.2"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"               % "2.5.23",
  "com.typesafe.akka"         %% "akka-http"                % "10.1.8",
  "com.typesafe.akka"         %% "akka-http-spray-json"     % "10.1.8",
  "com.typesafe.akka"         %% "akka-stream"              % "2.5.23",
  "org.apache.logging.log4j"  % "log4j-api"                 % "2.12.0",
  "org.apache.logging.log4j"  % "log4j-core"                % "2.12.0"
)

assemblyJarName in assembly := "producer.jar"