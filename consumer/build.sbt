name := "consumer"

version := "0.2"

scalaVersion := "2.12.0"

val akkaActorVersion : String  = "2.5.23"
val akkaHttpVersion  : String  = "10.1.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"               % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-stream"              % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-persistence"         % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-persistence-query"   % akkaActorVersion,
  "com.typesafe.akka"         %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"         %% "akka-http-spray-json"     % akkaHttpVersion,
  "org.apache.kafka"          %% "kafka"                    % "2.3.0",
  "io.spray"                  %% "spray-json"               % "1.3.5",
  "org.fusesource.leveldbjni" % "leveldbjni-all"            % "1.8"
)

assemblyJarName in assembly := s"consumer-${version.value}.jar"
