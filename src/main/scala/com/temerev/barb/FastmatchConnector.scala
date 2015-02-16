package com.temerev.barb

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.miriamlaurel.fxcore.party.Party
import quickfix._
import quickfix.field._
import quickfix.fix42.MarketDataRequest

class FastmatchConnector extends Actor with Application with ActorLogging {

  val PARTY = Party("FM", Some("FastMatch"))
  val PATH_TO_SESSION_SETTINGS = "/fm-session.ini"
  val initiator = mkInitiator()
  val cracker = new MessageCracker(this)

  override def preStart(): Unit = {
    super.preStart()
    initiator.start()
  }

  override def postStop(): Unit = {
    super.postStop()
    initiator.stop()
  }

  def receive = Actor.emptyBehavior

  override def onCreate(sessionID: SessionID): Unit = ()

  override def fromAdmin(message: Message, sessionID: SessionID): Unit = ()

  override def toAdmin(message: Message, sessionID: SessionID): Unit = ()

  override def onLogon(sid: SessionID): Unit = {
  }

  override def onLogout(sessionID: SessionID): Unit = ()

  override def toApp(message: Message, sessionID: SessionID): Unit = ()

  override def fromApp(message: Message, sessionID: SessionID): Unit = {
    if (message.getHeader.getString(35) == "h") {
      val message = new MarketDataRequest()
      message.set(new MDReqID(UUID.randomUUID().toString))
      message.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES))
      message.set(new MarketDepth(0))
      message.set(new MDUpdateType(MDUpdateType.INCREMENTAL_REFRESH))
      message.set(new AggregatedBook(false))
      val group1 = new MarketDataRequest.NoMDEntryTypes
      group1.set(new MDEntryType(MDEntryType.BID))
      message.addGroup(group1)
      group1.set(new MDEntryType(MDEntryType.OFFER))
      message.addGroup(group1)
      val group2 = new MarketDataRequest.NoRelatedSym
      group2.set(new Symbol("ALL"))
      message.addGroup(group2)
      Session.sendToTarget(message, sessionID)
    }
    log.info(message.toString)
  }

/*
  def onMessage(message: MarketDataSnapshotFullRefresh, sid: SessionID): Unit = {
    val noEntries = if (message.isSetNoMDEntries) message.getNoMDEntries.getValue else 0
    val group = new MarketDataSnapshotFullRefresh.NoMDEntries
    val cp = CurrencyPair(message.getSymbol.getValue)
    val offers = for (i <- 1 to noEntries) yield {
      message.getGroup(i, group)
      val side = if (group.getMDEntryType.getValue == MDEntryType.BID) QuoteSide.Bid else QuoteSide.Ask
      val amount = BigDecimal(group.getDecimal(MDEntrySize.FIELD))
      val price = BigDecimal(group.getDecimal(MDEntryPx.FIELD))
      Order(cp, side, amount, price, PARTY)
    }
    val snapshot = Snapshot(cp, offers.toList, DateTime.now())
    log.info(snapshot.toString())
  }
*/

  private def mkInitiator(): Initiator = {
    val settings = new SessionSettings(FastmatchConnector.this.getClass.getResourceAsStream(PATH_TO_SESSION_SETTINGS))
    val storeFactory = new MemoryStoreFactory
    val logFactory = new ScreenLogFactory(settings)
    val msgFactory = new DefaultMessageFactory
    new SocketInitiator(this, storeFactory, settings, logFactory, msgFactory)
  }
}

object FastmatchConnector extends App {
  val system = ActorSystem("lp")
  val fm = system.actorOf(Props[FastmatchConnector])
}
