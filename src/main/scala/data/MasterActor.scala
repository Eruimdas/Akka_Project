package data

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, AllForOneStrategy, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.routing.RoundRobinPool
import data.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model.{DateFetcher, HistoryFetcher, InitialResponse, MessageList}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MasterActor extends Actor with ActorLogging {

  var pageList : ArrayBuffer[Int] = ArrayBuffer[Int]()


  override def receive: Receive = {

    case received : DateFetcher => {

      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = received.link + "?date=" + received.date))

      log.debug("A single http request has been sent to the server.")
      log.info("The client is running.")

      responseFuture
        .onComplete {
          case Success(HttpResponse(StatusCodes.OK, _, entity, _)) => {
            log.debug("The server connection has been made succesfully, and the data has been fetched.")
            for{
            pageNum <- Unmarshal(entity).to[InitialResponse].map(num => num.pageNumber)
            initialMessageList <- Unmarshal(entity).to[InitialResponse].map(x => x.messageList)
            } yield {
              log.debug("The page number is : " + pageNum)
              self !  HistoryFetcher(received.link+"?date=",pageNum,received.date,pageList)
              Thread.sleep(50)
              context.child("workerActors").get ! MessageList(initialMessageList)
            }

          }
          case Success(HttpResponse(status, _, _, _)) => {
            if (status == StatusCodes.BadRequest)
              system.log.error("A BadRequest Error has been occured please check the server.")

            else if (status == StatusCodes.BadGateway)
              system.log.error("A BadGateway Error has been occured please check the server.")

            else if (status == StatusCodes.Forbidden)
              system.log.error("A Forbidden Error has been occured please check the server.")

            else if (status == StatusCodes.InternalServerError)
              system.log.error("An InternalServerError has been occured please check the server.")

            else if (status == StatusCodes.RequestTimeout)
              system.log.error("A RequestTimeout Error has been occured please check the server.")

            else if (status == StatusCodes.TooManyRequests)
              system.log.error("A TooManyRequests Error has been occured please check the server.")

            else
              system.log.error("An error which is not fatal has been occured, please check the server.")

          }
          case Failure(_)   =>
            system.log.error("A Fatal error has ben occured. Please immediately check the server.")
        }
    }

    case myData: HistoryFetcher => {
      log.debug("Workers initialized.")
      val workerActors = context.actorOf(props = RoundRobinPool(myData.pageNumber+1).props(Props(classOf[WorkerActor])), "workerActors")
      context.watch(workerActors)
      (1 to myData.pageNumber).foreach(pageNum => {
        workerActors ! HistoryFetcher(myData.link, pageNum, myData.date,pageList)
        if(math.random < 0.1) {
          log.warning("THE SYSTEM HAS STOPPED WORKING BECAUSE OF A RANDOM REASON")
          self ! Restart
        }
      }
      )
      }

    case Terminated(workerActors)=> {
      system.log.debug("The client is terminated.")
      system.terminate()
    }

    case myPageNumber: Int => {
      pageList += myPageNumber
    }

    case receivedPageList : ArrayBuffer[Int] => receivedPageList.foreach(number => pageList += number)
  }

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 10) {
    case error: Exception => {
      log.warning("The master actor has crushed and restarted.")
      Restart
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.warning("The master actor will be restarted.")
    context.children.foreach { child =>
      context.unwatch(child)
      context.stop(child)
    }
    self ! pageList
  }

}
