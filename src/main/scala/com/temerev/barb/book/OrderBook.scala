package com.temerev.barb.book

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{Order, OrderKey, QuoteSide}
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
    val half = selectHalf(key.side)
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
        val half = selectHalf(key.side)
        val bucket = half(order.price) - key
        val updated = if (bucket.isEmpty) half - order.price else half + (order.price -> bucket)
        if (key.side == QuoteSide.Bid) new OrderBook(instrument, updated, offers, newById)
        else new OrderBook(instrument, bids, updated, newById)
      case None => this
    }
  }
  
  def select(party: Party): OrderBook = select(party, include = true, None)

  def remove(party: Party, side: QuoteSide.Value): OrderBook = select(party, include = false, side = Some(side))

  def remove(party: Party): OrderBook = select(party, include = false, None)

  def best(side: QuoteSide.Value): Iterable[Order] = {
    val half = selectHalf(side)
    if (half.isEmpty) Seq.empty else half(half.firstKey).values
  }

  private def select(party: Party, include: Boolean, side: Option[QuoteSide.Value]): OrderBook = {
    var book = OrderBook(instrument)
    val selector = (key: OrderKey) => if (include) key.party == party && side.getOrElse(key.side) == key.side
    else key.party != party && side.getOrElse(key.side) == key.side
    for (b <- byKey) if (selector(b._1)) book = book.addUpdate(b._2)
    book
  }

  private def selectHalf(side: QuoteSide.Value) = if (side == QuoteSide.Bid) bids else offers
}
