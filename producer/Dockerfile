FROM openjdk:8-jre-alpine
ENV PORT_TO_HOST = "8080"
ENV HOST_NAME = "localhost"
COPY /target/scala-2.12/producer.jar producer.jar
ENTRYPOINT java -jar producer.jar

