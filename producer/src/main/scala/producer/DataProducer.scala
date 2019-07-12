package producer

import java.util.Random

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import model.Formatters._
import model.{InitialResponse, Message, PageResponse}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.immutable._
import scala.concurrent.ExecutionContextExecutor


object DataProducer {

  val log : Logger = LogManager.getLogger("DataProducer")


  def main(args: Array[String]){

    val myConfig: Config = ConfigFactory.load()
    val myPort : String = myConfig.getString("producer.port")
    val myHost : String = myConfig.getString("producer.host")

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    log.debug("System logger has been initialized.")

    val myMessageList1 : List[Message] = List(Message("hasan1"),Message("deneme1"),Message("yasarcan1"))
    val myMessageList2 : List[Message] = List(Message("hasan"),Message("deneme"),Message("yasarcan"))
    val randomizer = new Random()
    val pageNumber =  200 //randomizer.nextInt(500) + 1

    log.info("randomized value is : " + pageNumber)

    val route:Route =
      path("fetcher") {
        parameters('date.as[String],'page.?) { (date, page) =>
          system.log.info("A message has been received" + page)
          if(page.isDefined){
            complete(PageResponse(date, page.get.toInt, myMessageList2))
          } else {
            complete(InitialResponse(date, myMessageList1, pageNumber))
          }
        }
      }

    log.debug("The route for PageResponse and InitialResponse has been initialized.")

    val healthRoute: Route =
      path("health") {
        complete(OK)
      }

    log.debug("The route for healthRoute has been initialized.")

    val routes: Route = route ~ healthRoute

    // `route` will be implicitly converted to `Flow` using `RouteResult.route2HandlerFlow`
    val bindingFuture = Http().bindAndHandle(routes, myHost.substring(2,myHost.length()), myPort.substring(2,myPort.length()).toInt)

  }
}
