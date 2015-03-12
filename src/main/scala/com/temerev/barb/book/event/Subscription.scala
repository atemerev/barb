package com.temerev.barb.book.event

import akka.actor.ActorRef
import com.miriamlaurel.fxcore.instrument.Instrument
import com.miriamlaurel.fxcore.party.Party
import quickfix.SessionID

case class Subscription(instrument: Option[Instrument], party: Option[Party], destination: ActorRef, sessionID: Option[SessionID] = None)

case class Subscribe(subscription: Subscription)

case class Unsubscribe(subscription: Subscription)
