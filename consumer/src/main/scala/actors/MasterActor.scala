package actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorLogging, Kill, OneForOneStrategy, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.persistence.PersistentActor
import akka.routing.RoundRobinPool

import consumer.DataConsumer.{executionContext, mat, consumerConfig, system}
import model.Formatters._
import model._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MasterActor extends PersistentActor with ActorLogging {

  var  processedPages : ArrayBuffer[Int] = ArrayBuffer[Int]()

  override def persistenceId: String = "persistentMaster"

  override def receiveCommand: Receive = {

    case receivedDate : DateFetcher => {

      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = receivedDate.link + "?date=" + receivedDate.date))

      log.debug("A single http request has been sent to the server.")
      log.info("The client is running.")

      responseFuture
        .onComplete {
          case Success(HttpResponse(status, _, entity, _)) => {
            if (status == StatusCodes.OK) {
              log.info("The server connection has been made succesfully, and the data has been fetched.")
              for {
                pageNum <- Unmarshal(entity).to[InitialResponse].map(num => num.pageNumber)
                initialMessageList <- Unmarshal(entity).to[InitialResponse].map(x => x.messageList)
              } yield {
                log.debug("The page number is : " + pageNum)
                self ! HistoryFetcher(receivedDate.link + "?date=", pageNum, receivedDate.date, processedPages)
                Thread.sleep(50)
                if (!processedPages.toArray.contains(0)) {
                  context.child("workerActors").get ! MessageList(initialMessageList)
                }
              }
            }

            else if (status == StatusCodes.BadRequest)
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

    case HistoryFetcher(date,pageNumber,link, historyPages) => {
      log.info("Workers initialized.")
      val workerActors = context.actorOf(props = RoundRobinPool(pageNumber+1).props(Props(classOf[WorkerActor])), "workerActors")
      context.watch(workerActors)
      (1 to pageNumber).foreach(pageNum => {
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
      //log.info(s"Page Number of $pageNumber has been completed.")
      processedPages += pageNumber
      log.info(s"The new pages list is $processedPages")
    }

    case Message(receivedResponse) => {
      if(receivedResponse.equals("done")){
        if(!processedPages.toArray.contains(0))
          processedPages += 0
      }
      log.info(s"The message is $receivedResponse")
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
    log.info("The master actor has been restarted.")
    val myDate : String = consumerConfig.getString("consumer.date")
    val myLink : String = consumerConfig.getString("consumer.link")
    self ! DateFetcher(myDate.substring(2,myDate.length()),myLink)
    super.postRestart(reason)
  }
}
