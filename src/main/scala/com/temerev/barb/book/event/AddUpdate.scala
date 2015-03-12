package com.temerev.barb.book.event

import com.miriamlaurel.fxcore.market.Order
import org.joda.time.DateTime

case class AddUpdate(order: Order, override val timestamp: DateTime) extends BookUpdateEvent {
  override def party = Some(order.key.party)

  override def instrument = Some(order.key.instrument)
}