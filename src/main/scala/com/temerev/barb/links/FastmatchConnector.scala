package com.temerev.barb.links

import java.util.UUID

import akka.actor._
import com.miriamlaurel.fxcore.instrument.CurrencyPair
import com.miriamlaurel.fxcore.market.{OrderKey, Order, QuoteSide}
import com.miriamlaurel.fxcore.party.Party
import com.temerev.barb.book.OrderBook
import com.temerev.barb.book.event.{ClearLp, Remove, AddUpdate}
import org.joda.time.DateTime
import quickfix._
import quickfix.field._
import quickfix.fix42.{MarketDataSnapshotFullRefresh, MarketDataIncrementalRefresh, MarketDataRequest, TradingSessionStatus}

class FastmatchConnector(bookManager: ActorRef) extends Actor with Application with ActorLogging {

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

  override def fromApp(message: Message, sessionID: SessionID): Unit = cracker.crack(message, sessionID)

  def onMessage(message: TradingSessionStatus, sessionID: SessionID): Unit = {
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
    group2.set(new Symbol("EUR/USD"))
    message.addGroup(group2)
    Session.sendToTarget(message, sessionID)
  }

  def onMessage(message: MarketDataIncrementalRefresh, sid: SessionID): Unit = {
    val noEntries = if (message.isSetNoMDEntries) message.getNoMDEntries.getValue else 0
    val group = new MarketDataIncrementalRefresh.NoMDEntries
    for (i <- 1 to noEntries) yield {
      message.getGroup(i, group)
      val mdType = group.getMDUpdateAction.getValue
      val cp = CurrencyPair(group.getSymbol.getValue)
      val side = if (group.getMDEntryType.getValue == MDEntryType.BID) QuoteSide.Bid else QuoteSide.Ask
      val sourceId = group.getMDEntryID.getValue
      val key = OrderKey(PARTY, cp, side, sourceId)
      if (mdType == MDUpdateAction.NEW || mdType == MDUpdateAction.CHANGE) {
        val amount = BigDecimal(group.getDecimal(MDEntrySize.FIELD))
        val price = BigDecimal(group.getDecimal(MDEntryPx.FIELD))
        val order = Order(key, amount, price)
        // todo get time from message
        bookManager ! AddUpdate(order, DateTime.now())
      } else {
        bookManager ! Remove(key, DateTime.now())
      }
    }
  }

  def onMessage(message: MarketDataSnapshotFullRefresh, sid: SessionID): Unit = {
    // todo timestamps
    bookManager ! ClearLp(PARTY, DateTime.now())

    val noEntries = if (message.isSetNoMDEntries) message.getNoMDEntries.getValue else 0
    val group = new MarketDataSnapshotFullRefresh.NoMDEntries
    val cp = CurrencyPair(message.getSymbol.getValue)
    val offers = for (i <- 1 to noEntries) yield {
      message.getGroup(i, group)
      val side = if (group.getMDEntryType.getValue == MDEntryType.BID) QuoteSide.Bid else QuoteSide.Ask
      val amount = BigDecimal(group.getDecimal(MDEntrySize.FIELD))
      val price = BigDecimal(group.getDecimal(MDEntryPx.FIELD))
      val orderId = group.getOrderID.getValue
      val order = Order(OrderKey(PARTY, cp, side, orderId), amount, price)
      // todo timestamp
      bookManager ! AddUpdate(order, DateTime.now())
    }
  }

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
