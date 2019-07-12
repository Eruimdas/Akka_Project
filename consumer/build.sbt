name := "consumer"

version := "0.2"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-http"                % "10.1.8",
  "com.typesafe.akka"         %% "akka-actor"               % "2.5.23",
  "com.typesafe.akka"         %% "akka-stream"              % "2.5.23",
  "com.typesafe.akka"         %% "akka-http-spray-json"     % "10.1.8",
  "org.apache.kafka"          %% "kafka"                    % "2.3.0",
  "io.spray"                  %% "spray-json"               % "1.3.5",
  "com.typesafe.akka"         %% "akka-persistence"         % "2.5.23",
  "com.typesafe.akka"         %% "akka-persistence-query"   % "2.5.23",
  "org.fusesource.leveldbjni" % "leveldbjni-all"            % "1.8"
)

assemblyJarName in assembly := "consumer.jar"
