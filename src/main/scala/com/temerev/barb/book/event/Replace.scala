package com.temerev.barb.book.event

import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.party.Party
import com.temerev.barb.book.OrderBook

case class Replace(lp: Party, ticker: Instrument, book: OrderBook) extends BookUpdateEvent {
  override def party: Option[Party] = Some(lp)
  override def instrument: Option[Instrument] = Some(ticker)
}