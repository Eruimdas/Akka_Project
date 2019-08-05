package actors

import java.util.Properties

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import configs.ConsumerConfig
import consumer.DataConsumer.{executionContext, mat, system}
import model._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

class MasterActor extends Actor with ActorLogging with ConsumerConfig {

  override def receive: Receive = {

    case theDate @ DateFetcher(date,link) => {

      val responseFuture: Future[HttpResponse] = sendHttpRequestWithDate(link,date)

     Source.fromFuture(responseFuture)
        .map ( futureHttpResponse =>
          if (futureHttpResponse.status == StatusCodes.OK) {
         Unmarshal(futureHttpResponse.entity).to[InitialResponse].map { initialResponse =>
           println(initialResponse.pageNumber)
           initialResponse.messageList.foreach(message => println(message))
           (1 to initialResponse.pageNumber).map { pageNum =>
             sendHttpRequestWithPage(link, date, pageNum.toString).map { receivedRequest =>
               if (receivedRequest.status == StatusCodes.OK) {
                 Unmarshal(receivedRequest.entity).to[PageResponse].map { pageResponse =>
                   pageResponse.messageList.foreach { message =>
                     println(message)
                   }
                 }
               }
             }
           }
         }
        }
          else {
            log.error(s"An error occured while sending the request.Status code is: ${futureHttpResponse.status}")
          }
        ).recover { case error => log.error(s"Please check the server. The error is: $error")}
        .runWith(Sink.ignore)

      log.debug(s"A http request sent to server for date: $date to get page number.")
    }
  }

  def sendHttpRequestWithDate(link: String,date: String): Future[HttpResponse] = {
   Http().singleRequest(HttpRequest(uri =s"$link?date=$date"))
  }

  def sendHttpRequestWithPage(link: String, date: String, pageNum: String): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = s"$link?date=$date&page=$pageNum"))

  case class KafkaProducerConfigs(brokerList: String = "localhost:9092")  {
    val properties = new Properties()
    properties.put("bootstrap.servers", brokerList)
    properties.put("key.serializer", classOf[StringSerializer])
    properties.put("value.serializer", classOf[StringSerializer])
  }

  def sendToCloud(producer: KafkaProducer[String, String], topic : String, message: Message): Unit = {
    val putRecord = new ProducerRecord[String, String](topic, message.toString())
    producer.send(putRecord)
  }

  def createKafkaProducer(): KafkaProducer[String , String] =
    new KafkaProducer[String, String](KafkaProducerConfigs().properties)
}

