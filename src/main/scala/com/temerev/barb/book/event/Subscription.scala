package com.temerev.barb.book.event

import akka.actor.ActorRef
import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.party.Party

case class Subscription(instrument: Option[Instrument], party: Option[Party], destination: ActorRef)

case class Subscribe(subscription: Subscription)

case class Unsubscribe(subscription: Subscription)
