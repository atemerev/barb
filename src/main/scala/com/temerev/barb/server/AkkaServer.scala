package com.temerev.barb.server

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import akka.actor.{Props, ActorSystem, Actor}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.stream.scaladsl.StreamTcp._
import akka.stream.stage.{Directive, Context, StageState, StatefulStage}
import akka.util.ByteString

import scala.concurrent.Future

class AkkaServer extends Actor {

  implicit val system = context.system
  implicit val materializer = ActorFlowMaterializer()
  val localhost = new InetSocketAddress("127.0.0.1", 6466)
  val connections: Source[IncomingConnection, Future[ServerBinding]] = StreamTcp().bind(localhost)

  val flow = new StatefulStage[ByteString, FixField] {

    import ByteState._
    var byteState = Garbage
    var tagBuffer = ByteBuffer.allocate(255)
    var valBuffer = ByteBuffer.allocate(65535)
    var parsed = Vector[FixField]()

    override def initial: StageState[ByteString, FixField] = new State {

      override def onPush(chunk: ByteString, ctx: Context[FixField]): Directive = {
        for (b <- chunk) {
          byteState match {
            case Garbage => if (isNumber(b)) {
              tagBuffer.put(b)
              byteState = Tagnum
            }
            case Tagnum => if (isNumber(b)) {
              tagBuffer.put(b)
            } else if (isEqualsSign(b)) {
              tagBuffer.flip()
              byteState = Equals
            } else {
              fault()
            }
            case Equals => if (isSplit(b)) {
              fault()
            } else {
              valBuffer.put(b)
              byteState = Val
            }
            case Val => if (isSplit(b)) {
              valBuffer.flip()
              emitField()
              byteState = Split
            } else {
              valBuffer.put(b)
            }
            case Split => if (isNumber(b)) {
              tagBuffer.put(b)
              byteState = Tagnum
            } else {
              fault()
            }
          }
        }
        val result = parsed
        parsed = Vector[FixField]()
        emit(result.iterator, ctx)
      }
    }

    private def emitField(): Unit = {
      val tagBytes = new Array[Byte](tagBuffer.remaining())
      val valBytes = new Array[Byte](valBuffer.remaining())
      tagBuffer.get(tagBytes)
      valBuffer.get(valBytes)
      val tagNum = new String(tagBytes).toInt
      val value = new String(valBytes)
      val field = FixField(tagNum, value)
      tagBuffer.clear()
      valBuffer.clear()
      parsed = parsed :+ field
    }

    private def isNumber(b: Byte) = b >= 0x30 && b <= 0x39

    private def isEqualsSign(b: Byte) = b == 0x3D

    private def isSplit(b: Byte) = b == 0x20

    private def fault() {
      tagBuffer.clear()
      valBuffer.clear()
      byteState = ByteState.Garbage
    }

  }


  object ByteState extends Enumeration {
    val Garbage, Tagnum, Equals, Val, Split = Value
  }

  connections.runForeach(_.handleWith(Flow[ByteString]
    .transform(() => flow)
    .map(_.toString + "\n")
    .map(ByteString(_))))

  override def receive = Actor.emptyBehavior
}

object AkkaServer extends App {
  val system = ActorSystem("fix-server")
  system.actorOf(Props[AkkaServer])
}
