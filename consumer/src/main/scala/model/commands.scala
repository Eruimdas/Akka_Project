package model

sealed trait Command

case class WorkDoneResponse(pageNumber : Int) extends Command


case class DateFetcher(date : String,
                       link : String) extends Command