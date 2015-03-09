package com.temerev.barb.book

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{OrderKey, Order, QuoteSide}
import com.miriamlaurel.fxcore.party.Party

import scala.collection.immutable.TreeMap

case class OrderBook(
                 instrument: Instrument,
                 bids: TreeMap[BigDecimal, Map[OrderKey, Order]] = TreeMap.empty(new Ordering[BigDecimal]() {
                   override def compare(x: BigDecimal, y: BigDecimal): Int = y.compare(x)
                 }),
                 offers: TreeMap[BigDecimal, Map[OrderKey, Order]] = TreeMap.empty,
                 byKey: Map[OrderKey, Order] = Map.empty) {

  def addUpdate(order: Order): OrderBook = {
    val key = order.key
    val half = if (key.side == QuoteSide.Bid) bids else offers
    val newBucket = half.getOrElse(order.price, Map.empty) + (key -> order)
    val (newBids, newOffers) = byKey.get(key) match {
      case Some(prevOrder) =>
        val prevBucket = half(prevOrder.price) - key
        val updated = if (prevBucket.isEmpty) half - prevOrder.price else half + (prevOrder.price -> prevBucket)
        if (order.key.side == QuoteSide.Bid) (updated, offers) else (bids, updated)
      case None => if (key.side == QuoteSide.Bid) (bids + (order.price -> newBucket), offers)
                   else (bids, offers + (order.price -> newBucket))
    }
    val newById = byKey + (key -> order)
    OrderBook(instrument, newBids, newOffers, newById)
  }

  def remove(key: OrderKey): OrderBook = {
    val maybeOrder = byKey.get(key)
    maybeOrder match {
      case Some(order) =>
        val newById = byKey - key
        val half = if (key.side == QuoteSide.Bid) bids else offers
        val bucket = half(order.price) - key
        val updated = if (bucket.isEmpty) half - order.price else half + (order.price -> bucket)
        if (key.side == QuoteSide.Bid) new OrderBook(instrument, updated, offers, newById)
        else new OrderBook(instrument, bids, updated, newById)
      case None => this
    }
  }

  def remove(party: Party, side: QuoteSide.Value): OrderBook = {
    var book = this
    val half = if (side == QuoteSide.Bid) bids else offers
    val toRemove = half.values.flatten.map(_._1).filter(_.party == party)
    for (offer <- toRemove) book = book.remove(offer)
    book
  }

  def remove(party: Party): OrderBook = remove(party, QuoteSide.Bid).remove(party, QuoteSide.Ask)
}
