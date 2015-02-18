package com.temerev.barb

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.market.{QuoteSide, Order}
import com.miriamlaurel.fxcore.party.Party

import scala.collection.mutable

class OrderBook(instrument: Instrument) {

  val bids = mutable.TreeSet[Order]()
  val offers = mutable.TreeSet[Order]()

  def addUpdate(order: Order): Unit = {
    val to = if (order.side == QuoteSide.Bid) bids else offers
    if (to.contains(order)) {
      to.remove(order)
    }
    to.add(order)
    println("Bids size: " + bids.size)
    println("Offers size: " + bids.size)
  }

  def remove(source: Party, sourceId: String, side: QuoteSide.Value): Unit = {
    val fakeOrder = Order(instrument, side, 1, 1, source, Some(sourceId))
    val from = if (side == QuoteSide.Bid) bids else offers
    from.remove(fakeOrder)
    println("Bids size: " + bids.size)
    println("Offers size: " + bids.size)
  }
}
