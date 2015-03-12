package com.temerev.barb.links

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.hotspotfx.apiclient.{HsfxClient, HsfxMsgListener}
import com.hotspotfx.message.{HsfxMessage, Market}
import com.miriamlaurel.fxcore.instrument.CurrencyPair
import com.miriamlaurel.fxcore.market.{Order, OrderKey, QuoteSide}
import com.miriamlaurel.fxcore.party.Party
import com.temerev.barb.book.OrderBook
import com.temerev.barb.book.event.Replace
import com.typesafe.config.ConfigFactory

class HsfxConnector(bookManager: ActorRef) extends Actor with ActorLogging {
  val PARTY = Party("HSFX")
  val PATH_TO_SESSION_SETTINGS = "hsfx-session.conf"
  val config = ConfigFactory.load(PATH_TO_SESSION_SETTINGS)

  val client = new HsfxClient(config.getString("hsfx.host"), config.getInt("hsfx.port"),
    config.getString("hsfx.login"), config.getString("hsfx.password"), new HsfxMsgListener {
    override def onMessage(msg: HsfxMessage): Unit = self ! msg
  })

  override def postStop(): Unit = {
    client.disconnect()
    super.postStop()
  }

  override def receive = {
    case m: Market =>
      val instrument = CurrencyPair(m.currpair)
      var book = OrderBook(instrument)
      var c = 0
      m.bids.foreach(p => {
        val order = Order(OrderKey(PARTY, instrument, QuoteSide.Bid, PARTY.id + ":" + m.seq + ":b:" + c), BigDecimal(p.quantity), BigDecimal(p.price))
        book = book.addUpdate(order)
        c += 1
      })
      c = 0
      m.offers.foreach(p => {
        val order = Order(OrderKey(PARTY, instrument, QuoteSide.Ask, PARTY.id + ":" + m.seq + ":o:" + c), BigDecimal(p.quantity), BigDecimal(p.price))
        book = book.addUpdate(order)
        c += 1
      })
      bookManager ! Replace(PARTY, instrument, book)
  }
}
