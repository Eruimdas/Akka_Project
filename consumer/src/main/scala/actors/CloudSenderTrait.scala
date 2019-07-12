package actors

import com.typesafe.config.ConfigFactory

trait CloudSenderTrait{
  val topic: String = ConfigFactory.load().getString("kafka.topic")
}