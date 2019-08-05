package consumer

import java.util.Properties

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import configs.{ActorNameTrait, CloudSenderTrait, ConsumerConfig}
import model.{InitialResponse, Message, PageResponse}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.logging.log4j.{LogManager, Logger}
import util.Extensions.FutureOps

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

object DataConsumer extends ConsumerConfig with ActorNameTrait with CloudSenderTrait{

  implicit val log: Logger = LogManager.getLogger(DataConsumer)

  implicit val system: ActorSystem = ActorSystem()
  implicit val mat : Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  var processedPages : Set[Int] = Set[Int]()

  private val producer = createKafkaProducer()

  def main(args: Array[String]): Unit = {
  log.info("Program started.")

      Source.fromFuture(sendHttpRequestWithDate(linkForServer, dateOfLink).logOnFailure)
      .map { futureHttpResponse =>
      log.debug(s"The first response code is: ${futureHttpResponse.status}")
      if (futureHttpResponse.status == StatusCodes.OK) {
        fetchPageNumber(futureHttpResponse)
          .map( pageNumber => createSourceFromPageNumber(pageNumber))
      }
    }.runWith(Sink.ignore)
  }

  def createSourceFromPageNumber(pageNumber: Int): Unit= {
    log.info(s"page number $pageNumber is received.")
    Source(1 to pageNumber).map { pageNum =>
      createRestartableSourceForPage(pageNum)
    }.runWith(Sink.ignore)
  }

  def createRestartableSourceForPage(pageNum : Int): Future[Done] = {
    log.debug(s"Restartable Source for page $pageNum is created.")
    RestartSource.onFailuresWithBackoff(
      minBackoff = 100.millisecond,
      maxBackoff = 30.seconds,
      randomFactor = 0.2,
      maxRestarts = 10) { () =>
      Source.fromFuture(sendHttpRequestWithPage(linkForServer, dateOfLink, pageNum))
        .map { receivedRequest =>
          if (receivedRequest.status == StatusCodes.OK) {
            log.debug(s"${receivedRequest.status} for page number: $pageNum")
            unmarshalToPageResponse(receivedRequest.entity).map { innerPageResponse =>
              innerPageResponse.messageList.foreach(message => sendToCloud(producer, topic, message))
              log.info(s"${innerPageResponse.messageList} for page $pageNum")
            }
          }
        }
    }.runWith(Sink.ignore)

  }

  def fetchPageNumber(firstPageResponse : HttpResponse): Future[Int] = {
    log.debug(s"The second response code is: ${firstPageResponse.status}")
    unmarshalToInitialResponse(firstPageResponse.entity).map { initialResponse =>
      if (!processedPages.contains(0)){
        initialResponse.messageList.foreach( message => sendToCloud(producer, topic, message))
        processedPages += 0
      }
      log.debug(s"${initialResponse.pageNumber}")
      initialResponse.pageNumber
    }
  }

  def fetchPageData(pageNumber : Int): Unit = {
    Source(1 to pageNumber).map { pageNum =>
      if (!processedPages.contains(pageNum)){
      sendHttpRequestWithPage(linkForServer, dateOfLink, pageNum).map { receivedRequest =>
        log.debug(s"${receivedRequest.status}")
        unmarshalToPageResponse(receivedRequest.entity).map { innerPageResponse =>
          innerPageResponse.messageList.foreach(message => sendToCloud(producer, topic, message))
          log.debug(s"${innerPageResponse.messageList}")
        }
        }
        log.info(s"$processedPages")

        processedPages += pageNum
      }
    }
  }

  def unmarshalToInitialResponse(entity: HttpEntity): Future[InitialResponse] =
    Unmarshal(entity).to[InitialResponse]

  def unmarshalToPageResponse(entity: HttpEntity) : Future[PageResponse] =
    Unmarshal(entity).to[PageResponse]

  def sendHttpRequestWithDate(link: String,date: String): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri =s"$link?date=$date"))

  def sendHttpRequestWithPage(link: String, date: String, pageNum: Int): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = s"$link?date=$date&page=$pageNum"))

  def sendToCloud(producer: KafkaProducer[String, String], topic : String, message: Message): Unit = {
    val putRecord = new ProducerRecord[String, String](topic, message.toString)
    producer.send(putRecord)
  }

  def createKafkaProducer(): KafkaProducer[String , String] =
    new KafkaProducer[String, String](KafkaProducerConfigs().properties)
  }

case class KafkaProducerConfigs(brokerList: String = "localhost:9092")  {
  val properties = new Properties()
  properties.put("bootstrap.servers", brokerList)
  properties.put("key.serializer", classOf[StringSerializer])
  properties.put("value.serializer", classOf[StringSerializer])
}
