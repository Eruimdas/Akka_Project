package model

case class HistoryFetcher(date       : String,
                          pageNumber : Int,
                          link       : String,
                          pageList   : Set[Int],
                          messageList: List[Message])

case class dataToBeSent(messages : List[Message])

case class CloudSenderFinished(pageNumber : Int)
