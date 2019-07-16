package producer

import java.util.Random

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import configs.{MessageListTrait, ProducerConfig}
import model.{InitialResponse, PageResponse}
import org.apache.logging.log4j.LogManager

import scala.concurrent.ExecutionContextExecutor


object DataProducer extends ProducerConfig with MessageListTrait {

  val log = LogManager.getLogger(DataProducer)

  def main(args: Array[String]){

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    log.debug("System logger has been initialized.")

    val randomizer = new Random()
    val pageNumber =  200 //randomizer.nextInt(500) + 1

    log.info("randomized value is : " + pageNumber)

    val route:Route =
      path("fetcher") {
        parameters('date.as[String],'page.?) { (date, page) =>
          log.info("A message has been received" + page)

          page.fold(complete(InitialResponse(date, dummyMessageList1, pageNumber))){ page =>
            complete(PageResponse(date, page.toInt, dummyMessageList2))
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
    val bindingFuture = Http().bindAndHandle(routes, hostForProducer, portForProcuder.toInt)

  }
}
