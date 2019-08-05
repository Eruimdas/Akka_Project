package configs

import com.typesafe.config.{Config, ConfigFactory}

trait ConsumerConfig {
  val consumerConfig : Config = ConfigFactory.load()
  val dateOfLink : String = consumerConfig.getString("consumer.date")
  val linkForServer : String = consumerConfig.getString("consumer.link")
}
