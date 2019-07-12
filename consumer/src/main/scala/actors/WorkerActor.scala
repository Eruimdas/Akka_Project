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

  var pageNum: Int = 0

  val cloudSender: ActorRef = context.actorOf(props = Props(classOf[CloudSender]))

  def receive: Receive = {

    case receivedHistory @ HistoryFetcher(date,pageNumber,link,pageList) => {

      pageNum = pageNumber
      //log.info("Message has been received worker: " + pageNum)
      val myPageList = pageList.toArray

      if(!myPageList.contains(pageNum)) {
        log.info(s"$pageNum is going to be processed.")
        Http().singleRequest(HttpRequest(uri = link + date + "&page=" + pageNumber))
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

    case MessageList(messages) => {
      log.info("Message has been received worker: " + pageNum)
      cloudSender ! messages
    }
  }
}
