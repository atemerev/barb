package com.temerev.barb.book.event

import com.miriamlaurel.fxcore.party.Party
import org.joda.time.DateTime

case class ClearLp(clearParty: Party, override val timestamp: DateTime) extends BookUpdateEvent {
  override def party: Option[Party] = Some(clearParty)
  override def instrument = None
}