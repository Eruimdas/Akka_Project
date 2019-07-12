package consumer

import actors.MasterActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer}
import model.DateFetcher

import scala.concurrent.ExecutionContextExecutor

object DataConsumer extends ConsumerConfig {

  val myData : DateFetcher = DateFetcher(dateOfLink,linkForServer)

  implicit val system: ActorSystem = ActorSystem()
  implicit val mat : Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {

    val myMaster: ActorRef = system.actorOf(Props(classOf[MasterActor]),"masterActor")
    myMaster ! myData
    }
  }
