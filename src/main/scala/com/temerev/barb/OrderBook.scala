package com.temerev.barb

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{QuoteSide, Order}
import com.miriamlaurel.fxcore.party.Party

import scala.collection.immutable.TreeMap

class OrderBook(instrument: Instrument) {

  var byId = Map.empty[String, Order]
  var bids = TreeMap.empty[BigDecimal, Map[String, Order]]
  var offers = TreeMap.empty[BigDecimal, Map[String, Order]]

  def addUpdate(order: Order): Unit = {
    val orderId = order.sourceId.get
    val half = if (order.side == QuoteSide.Bid) bids else offers
    val newBucket = half.getOrElse(order.price, Map.empty) + (orderId -> order)
    byId = byId + (orderId -> order)
    byId.get(orderId) match {
      case Some(prevOrder) =>
        val prevBucket = half(prevOrder.price) - orderId
        if (order.side == QuoteSide.Bid) {
          if (prevBucket.isEmpty) bids = bids - prevOrder.price else bids = bids + (prevOrder.price -> prevBucket)
          bids = bids + (order.price -> newBucket)
        } else {
          if (prevBucket.isEmpty) offers = offers - prevOrder.price else offers = offers + (prevOrder.price -> prevBucket)
          offers = offers + (order.price -> newBucket)
        }
      case None => if (order.side == QuoteSide.Bid) {
        bids = bids + (order.price -> newBucket)
      } else {
        offers = offers + (order.price -> newBucket)
      }
    }
    // todo remove
    println("Bids size: " + bids.size)
    println("Offers size: " + bids.size)
  }

  def remove(source: Party, sourceId: String, side: QuoteSide.Value): Unit = {
    val maybeOrder = byId.get(sourceId)
    maybeOrder match {
      case Some(order) =>
        byId = byId - sourceId
        if (side == QuoteSide.Bid) {
          val bucket = bids(order.price) - sourceId
          bids = bids + (order.price -> bucket)
        } else {
          val bucket = offers(order.price) - sourceId
          offers = offers + (order.price -> bucket)
        }
        // todo replace with logs
      case None => println("Warning: unknown id received: " + sourceId)
    }
  }
}
