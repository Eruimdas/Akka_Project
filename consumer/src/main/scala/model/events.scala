package model

sealed trait Event

case class WorkDoneEvent(pageNumber: Int) extends Event