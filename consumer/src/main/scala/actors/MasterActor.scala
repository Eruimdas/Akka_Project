package actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorLogging, Kill, OneForOneStrategy, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.persistence.PersistentActor
import akka.routing.RoundRobinPool
import configs.ConsumerConfig
import consumer.DataConsumer.{executionContext, mat, system}
import model._

import scala.concurrent.Future


class MasterActor extends PersistentActor with ActorLogging with ConsumerConfig {

  var processedPages : Set[Int] = Set[Int]()

  override def persistenceId: String = "persistentMaster"

  override def receiveCommand: Receive = {

    case theDate @ DateFetcher(date,link) => {

      val responseFuture: Future[HttpResponse] = sendHttpRequestWithDate(link,date)

      log.debug(s"A single http request has been sent to the server for the link: DATE")

      responseFuture.map{response =>
        if (response.status == StatusCodes.OK) {
          log.info("The server connection has been made succesfully, and the data has been fetched.")
          workerActorCreator(response.entity, theDate)
        } else {
          log.error("An error occured while sending the request.Status code is:" + response.status)
        }
      }.recover { case error => log.error(s"Please check the server. The error is: $error") }

    }

    case HistoryFetcher(date,pageNumber,link, historyPages, messageList) => {

      val workerActors = context.actorOf(props = RoundRobinPool(pageNumber+1)
        .props(Props(classOf[WorkerActor])))
      context.watch(workerActors)

      if (!processedPages.contains(0)) {
        workerActors ! MessageList(messageList)
      }

      (1 to pageNumber).foreach { page =>
        if (!processedPages.contains(page)) {
          workerActors ! HistoryFetcher(link, page, date, historyPages, messageList)
        }
      }
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
    case WorkDoneEvent(pageNumber) if !processedPages.toArray.contains(pageNumber) => {
      processedPages += pageNumber
      log.info(s"The messages has been recovered: $pageNumber and processedPages is: $processedPages")
    }
  }

  override val supervisorStrategy : OneForOneStrategy = OneForOneStrategy() {
    case error: Exception => {
      log.info(s"The worker actor has crushed. The error is: $error" )
      Restart
    }

    case error => {
      log.info(s"The worker has stopped because of an unknown error. The error is: $error")
      Restart
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"The master actor will be restarted. Because of error: $reason")
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

  def sendHttpRequestWithDate(link: String,date: String): Future[HttpResponse] = {
   Http().singleRequest(HttpRequest(uri = link + "?date=" + date))
  }

  def workerActorCreator(askedEntity: ResponseEntity, theDateFetcher: DateFetcher): Unit =
    Unmarshal(askedEntity).to[InitialResponse].foreach { response =>
      log.debug("The page number is : " + response.pageNumber)
      self ! HistoryFetcher(theDateFetcher.link + "?date=", response.pageNumber,
        theDateFetcher.date, processedPages, response.messageList)
    }
}
