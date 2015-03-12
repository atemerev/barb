package com.temerev.barb.arbitrage

import akka.actor.{ActorLogging, Actor}
import com.temerev.barb.arbitrage.event.ArbitrageEvent

class ArbitrageMonitor extends Actor with ActorLogging {
  override def receive = {
    case ArbitrageEvent(first, second) => log.info("Arbitrage opportunity: %s <-> %s %s bid=%f ask=%f".format(
      first.key.party, second.key.party, first.key.instrument, first.price, second.price))
  }
}