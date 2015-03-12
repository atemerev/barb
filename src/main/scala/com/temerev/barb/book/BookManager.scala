package com.temerev.barb.book

import akka.actor.{Actor, ActorLogging}
import com.miriamlaurel.fxcore.instrument.Instrument
import com.temerev.barb.book.event._

import scala.collection.mutable

class BookManager extends Actor with ActorLogging {

  val books = mutable.AnyRefMap[Instrument, OrderBook]()
  val subscriptions = mutable.Set[Subscription]()

  def receive = {
    case Subscribe(s) => subscriptions += s
    case Unsubscribe(s) => subscriptions -= s
    case upd @ AddUpdate(order, timestamp) =>
      val instrument = order.key.instrument
      val book = books.getOrElseUpdate(instrument, OrderBook(instrument)).addUpdate(order)
      books += instrument -> book
      broadcast(upd)
    case upd @ Remove(key, timestamp) =>
      val instrument = key.instrument
      books.get(instrument) match {
        case Some(book) =>
          val newBook = book.remove(key)
          books += instrument -> newBook
          broadcast(upd)
        case None => log.warning("No order found for remove attempt: " + key)
      }
  }

  private def broadcast(update: BookUpdateEvent): Unit = {
    for (s <- subscriptions) {
      // broadcast to all if instrument or party is empty, otherwise broadcast by instrument and/or party
      if (s.instrument.isEmpty || update.instrument.isEmpty || s.instrument == update.instrument) {
        s.party match {
          case Some(p) => if (update.party.isEmpty || update.party.get == p) s.destination ! update
          case None => s.destination ! update
        }
      }
    }
  }
}
