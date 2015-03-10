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
    case AddUpdate(order) =>
      val instrument = order.key.instrument
      val book = books.getOrElseUpdate(instrument, OrderBook(instrument)).addUpdate(order)
      books += instrument -> book
      broadcast(book)
    case Remove(key) =>
      val instrument = key.instrument
      books.get(instrument) match {
        case Some(book) => {
          val newBook = book.remove(key)
          books += instrument -> newBook
          broadcast(newBook)
        }
        case None => log.warning("No order found for remove attempt: " + key)
      }
  }

  private def broadcast(book: OrderBook): Unit = {
    for (s <- subscriptions) {
      if (s.instrument.getOrElse(book.instrument) == book.instrument) {
        s.party match {
          case Some(p) => ???
          case None => s.destination ! book
        }
      }
    }
  }
}
