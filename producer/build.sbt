name := "producer"

version := "0.2"

scalaVersion := "2.12.0"

val akkaActorVersion : String  = "2.5.23"
val akkaHttpVersion  : String  = "10.1.8"
val apacheLogVersion : String  = "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"               % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-stream"              % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"         %% "akka-http-spray-json"     % akkaHttpVersion,
  "org.apache.logging.log4j"  % "log4j-api"                 % apacheLogVersion,
  "org.apache.logging.log4j"  % "log4j-core"                % apacheLogVersion
)

assemblyJarName in assembly := s"producer-${version.value}.jar"