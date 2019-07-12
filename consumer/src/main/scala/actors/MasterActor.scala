package actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorLogging, Kill, OneForOneStrategy, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.persistence.PersistentActor
import akka.routing.RoundRobinPool
import consumer.ConsumerConfig
import consumer.DataConsumer.{executionContext, mat, system}
import model.Formatters._
import model._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MasterActor extends PersistentActor with ActorLogging with ConsumerConfig {

  var processedPages : ArrayBuffer[Int] = ArrayBuffer[Int]()

  override def persistenceId: String = "persistentMaster"

  override def receiveCommand: Receive = {

    case DateFetcher(date,link) => {

      val responseFuture: Future[HttpResponse] = sendHttpRequestWithDate(link,date)

      log.debug("A single http request has been sent to the server.")
      log.info("The client is running.")

      responseFuture
        .onComplete {
          case Success(HttpResponse(status, _, entity, _)) => {
            if (status == StatusCodes.OK) {
              log.info("The server connection has been made succesfully, and the data has been fetched.")
              for {
                response <- Unmarshal(entity).to[InitialResponse]
                pageNum = response.pageNumber
                initialMessageList = response.messageList
              } yield {
                log.debug("The page number is : " + pageNum)
                self ! HistoryFetcher(link + "?date=", pageNum, date, processedPages)
                Thread.sleep(100)
                if (!processedPages.toArray.contains(0)) {
                  context.child("workerActors").get ! MessageList(initialMessageList)
                }
              }
            }

            else
              log.error(s"An error occured while sending the request.Status code is: $status")
          }
          case Failure(_)   =>
            log.error("A Fatal error has ben occured. Please immediately check the server.")
        }
    }

    case HistoryFetcher(date,pageNumber,link, historyPages) => {

      val workerActors = context.actorOf(props = RoundRobinPool(pageNumber+1).props(Props(classOf[WorkerActor])), "workerActors")
      context.watch(workerActors)
      (1 to pageNumber).foreach(pageNum => {
        log.info(s"Worker # $pageNum initialized.")
        if(!processedPages.toArray.contains(pageNum)) {
          workerActors ! HistoryFetcher(link, pageNum, date, historyPages)
        }
      }
      )
      }

    case Terminated(workerActors) => {
      system.log.info("The client is terminated.")
      self ! Kill
      system.terminate()
    }

    case WorkDoneResponse(pageNumber) => persist(WorkDoneEvent(pageNumber)){ event =>
      processedPages += pageNumber
      log.info(s"The new pages list is $processedPages")
    }

  }

  override def receiveRecover: Receive = {
    case WorkDoneEvent(pageNumber) => {
      if(!processedPages.toArray.contains(pageNumber))
      processedPages += pageNumber
    }
      log.info(s"The messages has been recovered: $pageNumber and processedPages is: $processedPages")
  }

  override val supervisorStrategy : OneForOneStrategy = OneForOneStrategy() {
    case error: Exception => {
      log.info(s"The master actor has crushed. The error is: $error" )
      Restart
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info("The master actor will be restarted.")
    context.children.foreach { child =>
      context.unwatch(child)
      context.stop(child)
    }
    super.preRestart(reason,message)
  }

  override def postRestart(reason: Throwable): Unit = {
    Thread.sleep(500)
    log.info(s"The master actor has been restarted because of an error: $reason")
    self ! DateFetcher(dateOfLink,linkForServer)
    super.postRestart(reason)
  }

  def sendHttpRequestWithDate(link: String,date: String): Future[HttpResponse] ={
   Http().singleRequest(HttpRequest(uri = link + "?date=" + date))
  }
}
