package com.temerev.barb

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{Order, QuoteSide}
import com.miriamlaurel.fxcore.party.Party

import scala.collection.immutable.TreeMap

case class OrderBook(
                 instrument: Instrument,
                 bids: TreeMap[BigDecimal, Map[String, Order]] = TreeMap.empty(new Ordering[BigDecimal]() {
                   override def compare(x: BigDecimal, y: BigDecimal): Int = y.compare(x)
                 }),
                 offers: TreeMap[BigDecimal, Map[String, Order]] = TreeMap.empty,
                 byId: Map[String, Order] = Map.empty) {

  def addUpdate(order: Order): OrderBook = {
    val orderId = order.sourceId.get
    val half = if (order.side == QuoteSide.Bid) bids else offers
    val newBucket = half.getOrElse(order.price, Map.empty) + (orderId -> order)
    val (newBids, newOffers) = byId.get(orderId) match {
      case Some(prevOrder) =>
        val prevBucket = half(prevOrder.price) - orderId
        val updated = if (prevBucket.isEmpty) half - prevOrder.price else half + (prevOrder.price -> prevBucket)
        if (order.side == QuoteSide.Bid) (updated, offers) else (bids, updated)
      case None => if (order.side == QuoteSide.Bid) (bids + (order.price -> newBucket), offers)
                   else (bids, offers + (order.price -> newBucket))
    }
    val newById = byId + (orderId -> order)
    OrderBook(instrument, newBids, newOffers, newById)
  }

  def remove(source: Party, sourceId: String, side: QuoteSide.Value): OrderBook = {
    val maybeOrder = byId.get(sourceId)
    maybeOrder match {
      case Some(order) =>
        val newById = byId - sourceId
        val half = if (order.side == QuoteSide.Bid) bids else offers
        val bucket = half(order.price) - sourceId
        val updated = if (bucket.isEmpty) half - order.price else half + (order.price -> bucket)
        if (side == QuoteSide.Bid) new OrderBook(instrument, updated, offers, newById)
        else new OrderBook(instrument, bids, updated, newById)
      case None => this
    }
  }
}
