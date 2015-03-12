package com.temerev.barb.book.event

import com.miriamlaurel.fxcore.market.OrderKey
import org.joda.time.DateTime

case class Remove(key: OrderKey, override val timestamp: DateTime) extends BookUpdateEvent {
  override def party = Some(key.party)

  override def instrument = Some(key.instrument)
}
