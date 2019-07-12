package producer

import model.Message

import scala.collection.immutable.List

trait messageListTrait {
  val myMessageList1 : List[Message] = List(Message("hasan1"),Message("deneme1"),Message("yasarcan1"))
  val myMessageList2 : List[Message] = List(Message("hasan"),Message("deneme"),Message("yasarcan"))
}