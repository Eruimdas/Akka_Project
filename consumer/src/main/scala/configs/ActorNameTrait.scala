package configs

import com.typesafe.config.{Config, ConfigFactory}

trait ActorNameTrait {
  val actorConfig : Config = ConfigFactory.load()
  val masterName : String = actorConfig.getString("actor.master.name")
}