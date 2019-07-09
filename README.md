## How to build

### Kafka Server

- Use `docker-compose up -d` for docker-compose.yml, to create kafka server. Edit `KAFKA_ADVERTISED_LISTENERS` and `KAFKA_LISTENERS` for port changes and `KAFKA_CREATE_TOPICS` for topics.

### Producer

- Use `sbt assembly` to create producer.jar in the directory.
- Then build docker image with:
```
docker build -t producer -f Dockerfile_producer.yml .
```
- Run the provider with.
```
docker run --network="host" --name theProvider producer
```
- If you want to change the server, edit the `application.conf` in producer.
```scala
//application.conf

producer{
  port = $port // ex: 8080
  host = $host // ex: "localhost"
}
```

### Consumer
- Use `sbt assembly` to create producer.jar in the directory.
- Then build docker image with:
```
docker build -t consumer -f Dockerfile_consumer.yml .
```
- Run the consumer with.
```
docker run --network="host" --name theConsumer consumer
```
- For any changes in the server, edit `application.conf` file.

```scala
// application.conf

kafka {
  topic = $topic // ex: "test"
}

consumer {
  //defaults
  date = $date // ex: "20190627"
  link = $link // ex: "http://localhost:8080/fetcher"
}

akka.persistence.journal.leveldb.dir = "target/journal"
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
```
