package model

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.mutable.ArrayBuffer

case class Message(message: String)

case class InitialResponse(date       : String,
                           messageList: List[Message],
                           pageNumber : Int)

case class PageResponse(date        : String,
                        pageNumber  : Int,
                        messageList : List[Message])

case class DateFetcher(date : String,
                       link : String) extends Command

case class HistoryFetcher(date       : String,
                          pageNumber : Int,
                          link       : String,
                          pageList   : ArrayBuffer[Int])

case class MessageList (messageList : List[Message])


case class WorkDoneResponse(pageNumber : Int) extends Command

case class WorkDoneEvent(pageNumber: Int) extends Event

case class dataToBeSent(messages : List[Message])

case class CloudSenderFinished(pageNumber : Int)

object Formatters extends DefaultJsonProtocol {
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat1(Message.apply)
  implicit val worketResponseFormat: RootJsonFormat[PageResponse] = jsonFormat3(PageResponse.apply)
  implicit val masterResponseFormat: RootJsonFormat[InitialResponse] = jsonFormat3(InitialResponse.apply)
  implicit val messageListFormat: RootJsonFormat[MessageList] = jsonFormat1(MessageList.apply)
}

sealed trait Event

sealed trait Command
