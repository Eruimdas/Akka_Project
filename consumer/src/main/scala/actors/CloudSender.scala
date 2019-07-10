package actors

import java.util.Properties

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import com.typesafe.config.ConfigFactory
import model.{Message, MessageList, PageResponse}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration._

class CloudSender extends Actor with ActorLogging{

  val producer = new KafkaProducer[String, String](KafkaProducerConfigs().properties)
  val topic: String = ConfigFactory.load("application.conf").getString("kafka.topic")
  context.setReceiveTimeout(50000 milliseconds)

  override def receive: Receive = {

    case received: PageResponse => {

      log.debug("Message has been received cloud: " + received.pageNumber)

      received.messageList.map(_.toString()).map { message =>
          new ProducerRecord[String, String](topic, message)}
        .map(myRecord => producer.send(myRecord))

      log.info("CLOUD SENDER HAS SENT THE VALUES TO THE CLOUD.!!!  " + received.pageNumber)
      log.debug("The sender has finished.")

      producer.close()
      sender ! Message("done")
      self ! PoisonPill
    }

    case ReceiveTimeout => {
      log.info("The Timeout has been completed. The Actor will be killed now.")
      producer.close()
      context.parent ! PoisonPill
      self ! PoisonPill
    }

    case receivedMessage : MessageList => {
      receivedMessage.messageList.map(_.toString()).map { message =>
          new ProducerRecord[String, String](topic, message)}
        .map(myRecord => producer.send(myRecord))

      log.info("CLOUD SENDER HAS SENT THE VALUES TO THE CLOUD.!!!  " + "0")
      log.debug("The sender has finished.")

      producer.close()
      sender ! Message("done")
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
