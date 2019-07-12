package producer

import com.typesafe.config.{Config, ConfigFactory}

trait ProducerConfig {
  val producerConfig: Config = ConfigFactory.load()
  val portForProcuder : String = producerConfig.getString("producer.port")
  val hostForProducer : String = producerConfig.getString("producer.host")
}