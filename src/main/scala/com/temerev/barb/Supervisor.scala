package com.temerev.barb

import akka.actor.{Props, Actor}
import com.temerev.barb.book.BookManager
import com.temerev.barb.links.{FastmatchConnector, HsfxConnector}

class Supervisor extends Actor {
  val bookManager = context.system.actorOf(Props[BookManager], "book-manager")
  val hsfxLink = context.system.actorOf(Props(classOf[HsfxConnector], bookManager), "link-hsfx")
  val fmLink = context.system.actorOf(Props(classOf[FastmatchConnector], bookManager), "link-fastmatch")

  override def receive = Actor.emptyBehavior
}