FROM openjdk:8-jre-alpine
COPY /target/scala-2.12/producer-0.2.jar producer.jar
ENV PORT_TO_HOST = "8080"
ENV HOST_NAME = "0.0.0.0"
ENTRYPOINT java -jar producer.jar
