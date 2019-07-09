package producer

import java.util.Random

import akka.actor.ActorSystem
import akka.event.LogSource
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import model.Formatters._
import model.{InitialResponse, Message, PageResponse}

import scala.collection.immutable._
import scala.concurrent.ExecutionContextExecutor

object DataProducer {

  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    implicit val logSource: LogSource[AnyRef]  = new LogSource[AnyRef] {
      def genString(o: AnyRef): String = o.getClass.getName
      override def getClazz(o: AnyRef): Class[_] = o.getClass
    }

    system.log.debug("System logger has been initialized.")

    val myMessageList1 : List[Message] = List(Message("hasan1"),Message("deneme1"),Message("yasarcan1"))
    val myMessageList2 : List[Message] = List(Message("hasan"),Message("deneme"),Message("yasarcan"))
    val randomizer = new Random()
    val pageNumber =  randomizer.nextInt(500) + 1
    system.log.info("randomized value is : " + pageNumber)

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

    system.log.debug("The route for PageResponse and InitialResponse has been initialized.")

    val healthRoute: Route =
      path("health") {
        complete(OK)
      }

    system.log.debug("The route for healthRoute has been initialized.")

    val routes: Route = route ~ healthRoute

    // `route` will be implicitly converted to `Flow` using `RouteResult.route2HandlerFlow`
    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  }
}
