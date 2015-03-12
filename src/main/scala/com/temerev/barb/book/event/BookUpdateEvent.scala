package com.temerev.barb.book.event

import com.miriamlaurel.fxcore.Timestamp
import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.party.Party

trait BookUpdateEvent extends Timestamp {
  def party: Option[Party]
  def instrument: Option[Instrument]
}
