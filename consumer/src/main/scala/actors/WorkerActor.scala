package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import consumer.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model._

class WorkerActor extends Actor with ActorLogging{

  val cloudSender: ActorRef = context.actorOf(props = Props(classOf[CloudSender]))

  def receive: Receive = {

    case receivedHistory @ HistoryFetcher(date,pageNum,link,pageList) => {

      //log.info("Message has been received worker: " + pageNum)
      val myPageList = pageList.toArray

      if(!myPageList.contains(pageNum)) {
        log.info(s"$pageNum is going to be processed.")
        Http().singleRequest(HttpRequest(uri = link + date + "&page=" + pageNum))
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

    case CloudSenderFinished(pageNumber) =>{
        context.actorSelection("akka://default/user/masterActor") ! WorkDoneResponse(pageNumber)
        log.info("worker has stopped.")
        self ! PoisonPill
    }

    case MessageList(messages) => {
      log.info("Message has been received worker: 0")
      cloudSender ! messages
    }
  }
}
