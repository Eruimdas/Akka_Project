FROM openjdk:8-jre-alpine
COPY /target/scala-2.12/consumer-0.2.jar consumer.jar
ENV DIR_FOR_PERSISTENCE = "journal"
ENV DATE_FOR_LINK = "20190614"
ENV KAFKA_TOPIC = "test"
ENV LINK_FOR_SERVER = "http://localhost:8080/fetcher"
ENV MASTER_ACTOR_NAME = "masterActor"
ENV MAX_OPEN_REQUESTS = 2048
ENTRYPOINT java -jar consumer.jar
