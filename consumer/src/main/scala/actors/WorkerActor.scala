package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.Timeout
import consumer.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model._

import scala.concurrent.duration.FiniteDuration

class WorkerActor extends Actor with ActorLogging{

  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  var pageNum: Int = 0

  val cloudSender: ActorRef = context.actorOf(props = Props(classOf[CloudSender]))

  def receive: Receive = {

    case receivedHistory: HistoryFetcher => {

      pageNum = receivedHistory.pageNumber
      //log.info("Message has been received worker: " + pageNum)
      val myPageList = receivedHistory.pageList.toArray

      if(!myPageList.contains(pageNum)) {
        log.info(s"$pageNum is going to be processed.")
        Http().singleRequest(HttpRequest(uri = receivedHistory.link + receivedHistory.date + "&page=" + receivedHistory.pageNumber))
          .flatMap(httpRes => Unmarshal(httpRes.entity).to[PageResponse])
          .map(myVal => cloudSender ! myVal)
          .recover {
            case error: Throwable => {
              log.error(s"There's an error while sending the request: $pageNum")
              Thread.sleep(50)
              self ! receivedHistory
            }
          }
      }
    }

    case Message(responseMessage) =>{

      if(responseMessage.equals("done")) {
        context.actorSelection("akka://default/user/masterActor") ! WorkDoneResponse(pageNum)
        log.info("worker has stopped.")
        self ! PoisonPill
      }

      else {
        log.warning("Unknown message sent to the WorkerActor from CloudSender")
      }
    }

    case myMessageList: MessageList => {
      log.info("Message has been received worker: " + pageNum)
      cloudSender ! myMessageList
    }
  }
}
