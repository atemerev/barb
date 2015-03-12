package com.temerev.barb

import akka.actor.{Props, Actor}
import com.temerev.barb.arbitrage.ArbitrageMonitor
import com.temerev.barb.book.BookManager
import com.temerev.barb.book.event.{Subscription, Subscribe}
import com.temerev.barb.links.{FastmatchConnector, HsfxConnector}

class Supervisor extends Actor {
  val hsfxLink = context.system.actorOf(Props(classOf[HsfxConnector], bookManager), "link-hsfx")
  val fmLink = context.system.actorOf(Props(classOf[FastmatchConnector], bookManager), "link-fastmatch")
  val arbMonitor = context.system.actorOf(Props[ArbitrageMonitor], "arbitrage-monitor")
  val bookManager = context.system.actorOf(Props(classOf[BookManager], arbMonitor), "book-manager")

  override def receive = Actor.emptyBehavior
}