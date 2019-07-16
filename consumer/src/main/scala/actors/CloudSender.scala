package actors

import java.util.Properties

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import configs.CloudSenderTrait
import model.{CloudSenderFinished, Message, MessageList, PageResponse}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration._

class CloudSender extends Actor with ActorLogging with CloudSenderTrait {

  val producer: KafkaProducer[String, String] = createKafkaProducer()
  context.setReceiveTimeout(10000 milliseconds)

  override def receive: Receive = {

    case  PageResponse(date,pageNumber,receivedPageResponse) => {

      log.debug("Message has been received cloud: " + pageNumber)

      receivedPageResponse.foreach { message =>
        sendToCloud(producer,topic,message)
      }


      log.info(s"Cloud sender has sent the values to the cloud: $pageNumber")
      log.debug(s"The sender $pageNumber has finished.")

      producer.close()
      sender ! CloudSenderFinished(pageNumber)
    }

    case ReceiveTimeout => {
      log.info("The Timeout has been completed. The Actor will be killed now.")
      producer.close()
      context.parent ! PoisonPill
    }

    case MessageList(receivedMessage) => {
      receivedMessage.foreach { message =>
        sendToCloud(producer,topic,message)
      }

      log.info("Cloud sender has sent the values to the cloud. Processed page is: 0.")
      log.debug("The sender 0 has finished.")

      producer.close()
      sender ! CloudSenderFinished(0)
      self ! PoisonPill
    }
  }

  case class KafkaProducerConfigs(brokerList: String = "localhost:9092")  {
    val properties = new Properties()
    properties.put("bootstrap.servers", brokerList)
    properties.put("key.serializer", classOf[StringSerializer])
    properties.put("value.serializer", classOf[StringSerializer])
  }

  def sendToCloud(producer: KafkaProducer[String, String], topic : String, message: Message): Unit ={
      val putRecord = new ProducerRecord[String, String](topic, message.toString())
      producer.send(putRecord)
  }

  def createKafkaProducer(): KafkaProducer[String , String] =
    new KafkaProducer[String, String](KafkaProducerConfigs().properties)


}


