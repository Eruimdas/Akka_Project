package consumer

import actors.MasterActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.LogSource
import akka.stream.{ActorMaterializer, Materializer}
import model.DateFetcher

import scala.concurrent.ExecutionContextExecutor

object DataConsumer extends consumerConfig {

  val myData : DateFetcher = DateFetcher(myDate.substring(2,myDate.length()),myLink)

  implicit val system: ActorSystem = ActorSystem()
  implicit val mat : Materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  def main(args: Array[String]): Unit = {

    val myMaster: ActorRef = system.actorOf(Props(classOf[MasterActor]),"masterActor")
    myMaster ! myData
    }
  }
