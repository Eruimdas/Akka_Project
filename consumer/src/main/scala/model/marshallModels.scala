package model

import spray.json._

case class Message(message: String)

case class InitialResponse(date       : String,
                           messageList: List[Message],
                           pageNumber : Int)

case class PageResponse(date        : String,
                        pageNumber  : Int,
                        messageList : List[Message])

case class MessageList (messageList : List[Message])

object Message extends DefaultJsonProtocol {
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat1(Message.apply)
}

object PageResponse extends DefaultJsonProtocol {
  import Message._
  implicit val workerResponseFormat: RootJsonFormat[PageResponse] = jsonFormat3(PageResponse.apply)
}

object InitialResponse extends DefaultJsonProtocol {
  import Message._
  implicit val masterResponseFormat: RootJsonFormat[InitialResponse] = jsonFormat3(InitialResponse.apply)
}

object MessageList extends DefaultJsonProtocol {
  import Message._
  implicit val messageListFormat: RootJsonFormat[MessageList] = jsonFormat1(MessageList.apply)
}
