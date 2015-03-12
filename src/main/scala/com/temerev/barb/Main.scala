package com.temerev.barb

import akka.actor.{Props, ActorSystem}

object Main extends App {
  val system = ActorSystem("barb")
  system.actorOf(Props[Supervisor], "root-supervisor")
}
