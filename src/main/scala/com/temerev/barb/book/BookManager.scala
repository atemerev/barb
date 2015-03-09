package com.temerev.barb.book

import akka.actor.{ActorLogging, Actor}
import com.miriamlaurel.fxcore.instrument.Instrument
import com.temerev.barb.book.event.{Remove, AddUpdate}

import scala.collection.mutable

class BookManager extends Actor with ActorLogging {

  val books = mutable.AnyRefMap[Instrument, OrderBook]()

  def receive = {
    case AddUpdate(order) =>
      val instrument = order.key.instrument
      val book = books.getOrElseUpdate(instrument, OrderBook(instrument))
      books += instrument -> book.addUpdate(order)
    case Remove(key) => {
      val instrument = key.instrument
      books.get(instrument) match {
        case Some(book) => books += instrument -> book.remove(key)
        case None => log.warning("No order found for remove attempt: " + key)
      }
    }
  }
}
