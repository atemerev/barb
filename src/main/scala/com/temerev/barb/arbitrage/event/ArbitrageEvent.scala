package com.temerev.barb.arbitrage.event

import com.miriamlaurel.fxcore.market.Order

case class ArbitrageEvent(first: Order, second: Order)
