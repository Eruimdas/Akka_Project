package data

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import data.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model.{HistoryFetcher, MessageList, PageResponse}


class WorkerActor extends Actor with ActorLogging{

  var pageNum: Int = -1
  val cloudSender: ActorRef = context.actorOf(props = Props(classOf[CloudSender]))

  def receive: Receive = {

    case dataList: HistoryFetcher => {

      pageNum = dataList.pageNumber
      log.debug("Message has been received worker: " + pageNum)
      val myPageList = dataList.pageList.toArray

      if(!myPageList.contains(pageNum)) {

        Http().singleRequest(HttpRequest(uri = dataList.link + dataList.date + "&page=" + dataList.pageNumber))
          .flatMap(httpRes => Unmarshal(httpRes.entity).to[PageResponse])
          .map(myVal => cloudSender ! myVal)
          .recover {
            case error: Throwable => {
              log.debug(s"There's an error while sending the request.  $error")
              self ! dataList
            }
          }
      }
    }

    case "done" =>{
      context.parent ! pageNum
      log.debug("worker has stopped.")
      self ! PoisonPill
    }
    case myMessageList: MessageList => {
      cloudSender ! myMessageList
    }
  }
}
