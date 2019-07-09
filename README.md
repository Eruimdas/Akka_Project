## How to build

### Application.conf

First configure the parameters.

```scala
kafka {
  topic = $topicName
}

consumer {

  date = $dateString
  link = $askedLink // ex: http://localhost:8080/fetcher
}
```

### Docker images

- Use `sbt assembly` to create producer.jar and consumer.jar
- Then build docker images with the provided Dockerfiles with:
```
docker build -t provider -f Dockerfile_provider.yml .
```
- Repeat the step for consumer.

### Running the docker images

- Use `docker-compose up -d` for docker-compose.yml, to create kafka server. Change `KAFKA_ADVERTISED_LISTENERS` and `KAFKA_LISTENERS` for port changes and `KAFKA_CREATE_TOPICS` for topics.
- Use `docker run --network="host" --name theProvider provider`.
- Use `docker run --network="host" --name theConsumer consumer`.
