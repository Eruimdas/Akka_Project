package data

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.LogSource
import akka.stream.{ActorMaterializer, Materializer}
import model.DateFetcher

import scala.concurrent.ExecutionContextExecutor

object DataConsumer {

  val myDate : DateFetcher = DateFetcher("20190627","http://localhost:8080/fetcher")// args[0],args[1]

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
    myMaster ! myDate

    }
  }
