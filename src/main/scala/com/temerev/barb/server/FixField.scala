package com.temerev.barb.server

case class FixField(tag: Int, value: String) {
  override def toString: String = s"$tag=$value"
}
