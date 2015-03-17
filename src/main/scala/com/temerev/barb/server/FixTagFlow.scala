package com.temerev.barb.server

import java.nio.ByteBuffer

import akka.stream.stage.{Directive, Context, StageState, StatefulStage}
import akka.util.ByteString

object FixTagFlow extends StatefulStage[ByteString, FixField] {

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
    val tagNum = tagBuffer.getInt
    val bytes = new Array[Byte](valBuffer.remaining())
    valBuffer.get(bytes)
    val value = new String(bytes)
    val field = FixField(tagNum, value)
    tagBuffer.clear()
    valBuffer.clear()
    parsed = parsed :+ field
  }

  private def isNumber(b: Byte) = b >= 0x30 && b <= 0x39

  private def isEqualsSign(b: Byte) = b == 0x3D

  private def isSplit(b: Byte) = b == 0x01

  private def fault() {
    tagBuffer.clear()
    valBuffer.clear()
    byteState = ByteState.Garbage
  }
}

object ByteState extends Enumeration {
  val Garbage, Tagnum, Equals, Val, Split = Value
}
