FROM openjdk:8-jre-alpine
COPY /target/scala-2.12/consumer.jar consumer.jar
ENV DIR_FOR_PERSISTENCE = "journal"
ENV DATE_FOR_LINK = "20190614"
ENV KAFKA_TOPIC = "test"
ENV LINK_FOR_SERVER = "http://0.0.0.0:8080/fetcher"
ENTRYPOINT java -jar consumer.jar
