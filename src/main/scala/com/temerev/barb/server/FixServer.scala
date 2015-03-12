package com.temerev.barb.server

import akka.actor.Actor
import com.miriamlaurel.fxcore.instrument.CurrencyPair
import com.miriamlaurel.fxcore.market.{Order, OrderKey, QuoteSide}
import com.miriamlaurel.fxcore.party.Party
import com.temerev.barb.book.event._
import com.temerev.barb.server.event.{Logout, Logon}
import quickfix.field.SecurityExchange
import quickfix.fix44.{MarketDataSnapshotFullRefresh, MarketDataIncrementalRefresh, MarketDataRequest}
import quickfix.fix44.MarketDataRequest.NoRelatedSym
import quickfix._

import scala.collection.mutable

class FixServer extends Actor with Application {

  val cracker = new MessageCracker(this)
  val subscriptions = mutable.Map[SessionID, Subscription]()

  override def receive = {
    case Subscribe(s) => subscriptions += s.sessionID.get -> s
    case Unsubscribe(s) => subscriptions -= s.sessionID.get
    case Logout(sid) => subscriptions -= sid
    case e: BookUpdateEvent =>
      val message = new MarketDataIncrementalRefresh
      e match {
        case AddUpdate(order, timestamp) => ???
        case Remove(key, timestamp) => ???
        case Replace(party, ticker, book) =>
          val message = new MarketDataSnapshotFullRefresh
          // todo fill the message
      }
      // todo send to subscribed sessions according to requested instruments / parties
  }

  def onMessage(message: MarketDataRequest, sid: SessionID): Unit = {
    val noRelatedSym = message.getNoRelatedSym.getValue
    val group = new NoRelatedSym
    for (i <- 1 to noRelatedSym) {
      message.getGroup(i, group)
      val symbol = group.getSymbol.getValue
      val instrument = Some(CurrencyPair(symbol))
      val party = if (group.isSetField(SecurityExchange.FIELD)) Some(Party(group.getSecurityExchange.getValue)) else None
      val subscription = Subscription(instrument, party, self, Some(sid))
      self ! Subscribe(subscription)
    }
  }

  override def onCreate(sessionID: SessionID): Unit = ()

  override def fromAdmin(message: Message, sessionID: SessionID): Unit = ()

  override def onLogon(sessionID: SessionID): Unit = self ! Logon(sessionID)

  override def onLogout(sessionID: SessionID): Unit = self ! Logout(sessionID)

  override def toApp(message: Message, sessionID: SessionID): Unit = ()

  override def fromApp(message: Message, sessionID: SessionID): Unit = cracker.crack(message, sessionID)

  override def toAdmin(message: Message, sessionID: SessionID): Unit = ()
}
