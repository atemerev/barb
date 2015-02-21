package com.temerev.barb

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{Order, QuoteSide}
import com.miriamlaurel.fxcore.party.Party

import scala.collection.immutable.TreeMap

class OrderBook(
                 val instrument: Instrument,
                 val bids: TreeMap[BigDecimal, Map[String, Order]] = TreeMap.empty(new Ordering[BigDecimal]() {
                   override def compare(x: BigDecimal, y: BigDecimal): Int = y.compare(x)
                 }),
                 val offers: TreeMap[BigDecimal, Map[String, Order]] = TreeMap.empty,
                 byId: Map[String, Order] = Map.empty) {

  def addUpdate(order: Order): OrderBook = {
    val orderId = order.sourceId.get
    val half = if (order.side == QuoteSide.Bid) bids else offers
    val newBucket = half.getOrElse(order.price, Map.empty) + (orderId -> order)
    val (newBids, newOffers) = byId.get(orderId) match {
      case Some(prevOrder) =>
        val prevBucket = half(prevOrder.price) - orderId
        if (order.side == QuoteSide.Bid) {
          val updated = if (prevBucket.isEmpty) bids - prevOrder.price else bids + (prevOrder.price -> prevBucket)
          (updated, offers)
        } else {
          val updated = if (prevBucket.isEmpty) offers - prevOrder.price else offers + (prevOrder.price -> prevBucket)
          (bids, updated)
        }
      case None => if (order.side == QuoteSide.Bid) {
        (bids + (order.price -> newBucket), offers)
      } else {
        (bids, offers + (order.price -> newBucket))
      }
    }
    val newById = byId + (orderId -> order)
    // todo remove
    println("Bids size: " + newBids.size)
    println("Offers size: " + newOffers.size)
    new OrderBook(instrument, newBids, newOffers, newById)
  }

  def remove(source: Party, sourceId: String, side: QuoteSide.Value): OrderBook = {
    val maybeOrder = byId.get(sourceId)
    maybeOrder match {
      case Some(order) =>
        val newById = byId - sourceId
        if (side == QuoteSide.Bid) {
          val bucket = bids(order.price) - sourceId
          val updated = if (bucket.isEmpty) bids - order.price else bids + (order.price -> bucket)
          new OrderBook(instrument, updated, offers, newById)
        } else {
          val bucket = offers(order.price) - sourceId
          val updated = if (bucket.isEmpty) offers - order.price else offers + (order.price -> bucket)
          new OrderBook(instrument, bids, updated, newById)
        }
      case None => this
    }
  }
}
