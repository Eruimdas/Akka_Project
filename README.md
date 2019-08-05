## How to build

### Kafka Server

- Use `docker-compose up -d` for docker-compose.yml, to create kafka server. Edit `KAFKA_ADVERTISED_LISTENERS` and `KAFKA_LISTENERS` for port changes and `KAFKA_CREATE_TOPICS` for topics.

### Producer

- Use `sbt assembly` to create producer.jar in the directory.
- Then build docker image with:
```
docker build -t producer:$tag .
```
- Run the provider with.
```
docker run --network="host" --name theProvider producer:$tag
```
- If you want to change the server, add the `-e PORT_TO_HOST = $newPort` and `-e HOST_NAME = $newHost` to docker run command.

### Consumer
- Use `sbt assembly` to create producer.jar in the directory.
- Then build docker image with:
```
docker build -t consumer:$tag .
```
- Run the consumer with.
```
docker run --network="host" --name theConsumer consumer:$tag
```
- For any changes in the server, add `-e DATE_FOR_LINK = $newDate` and `-e DIR_FOR_PERSISTENCE = $newDirectory` to docker run command.
