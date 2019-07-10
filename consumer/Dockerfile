FROM openjdk:8-jre-alpine

ENV DIR_FOR_PERSISTENCE = "journal"
ENV DATE_FOR_LINK = "20190614"
COPY /target/scala-2.12/consumer.jar consumer.jar
ENTRYPOINT java -jar consumer.jar
