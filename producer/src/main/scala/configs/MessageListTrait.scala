package configs

import model.Message

import scala.collection.immutable.List

trait MessageListTrait {
  val dummyMessageList1 : List[Message] = List(Message.apply("hasan1"),Message.apply("deneme1"),Message.apply("yasarcan1"))
  val dummyMessageList2 : List[Message] = List(Message.apply("hasan"),Message.apply("deneme"),Message.apply("yasarcan"))
}
