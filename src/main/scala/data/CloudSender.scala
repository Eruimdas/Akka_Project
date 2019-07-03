package data

import java.util.Properties

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import model.{MessageList, PageResponse}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration._

class CloudSender extends Actor with ActorLogging{

  val producer = new KafkaProducer[String, String](KafkaProducerConfigs().properties)
  context.setReceiveTimeout(10000 milliseconds)

  override def receive: Receive = {

    case received: PageResponse => {

      log.debug("Message has been received cloud: " + received.pageNumber)

      received
        .messageList
        .map(_.toString())
        .map { message =>
          new ProducerRecord[String, String]("test", message)
        }
        .map(myRecord => producer.send(myRecord))

      log.info("CLOUD SENDER HAS SENT THE VALUES TO THE CLOUD.!!!  " + received.pageNumber)
      log.debug("The sender has finished.")

      producer.close()
      sender ! "done"
      self ! PoisonPill
    }

    case ReceiveTimeout => {
      log.debug("The Timeout has been completed. The Actor will be killed now.")
      producer.close()
      context.parent ! PoisonPill
      self ! PoisonPill
    }

    case receivedMessage : MessageList => {
      receivedMessage
        .messageList
        .map(_.toString())
        .map { message =>
          new ProducerRecord[String, String]("test", message)
        }
        .map(myRecord => producer.send(myRecord))

      log.info("CLOUD SENDER HAS SENT THE VALUES TO THE CLOUD.!!!  " + "0")
      log.debug("The sender has finished.")

      producer.close()
      sender ! "done"
      self ! PoisonPill
    }
  }

  case class KafkaProducerConfigs(brokerList: String = "localhost:9092") {
    val properties = new Properties()
    properties.put("bootstrap.servers", brokerList)
    properties.put("key.serializer", classOf[StringSerializer])
    properties.put("value.serializer", classOf[StringSerializer])
  }
  }
