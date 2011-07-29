package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList

object MarketDepth {
  val Empty = new MarketDepth(Array[Double]())

  def apply(bidAsks: Array[Double], copy: Boolean = false) = {
    if (copy) {
      val x = new Array[Double](bidAsks.length)
      System.arraycopy(bidAsks, 0, x, 0, x.length)
      new MarketDepth(x)
    } else new MarketDepth(bidAsks)
  }
}

/**
 * 0 - bid price
 * 1 - bid size
 * 2 - ask price
 * 3 - ask size
 */
@serializable @cloneable
final class MarketDepth($bidAsks: Array[Double], $bidOrders : ArrayList[Double], $askOrders : ArrayList[Double]) {
  @transient var isChanged: Boolean = _

  def this(_bidAsks: Array[Double]) = this(_bidAsks, null, null)
  def this() = this(null, null, null)

  def bidOrders = $bidOrders
  def bidOrders_=(that: ArrayList[Double]) {
    for(bid <- that) {
      $bidOrders += bid
    }
  }

  def askOrders = $askOrders
  def askOrders_=(that: ArrayList[Double]) {
    for(ask <- that) {
      $askOrders += ask
    }
  }

  def depth = $bidAsks.length / 4
  
  def bidPrice(idx: Int) = $bidAsks(idx * 4)
  def bidSize (idx: Int) = $bidAsks(idx * 4 + 1)
  def askPrice(idx: Int) = $bidAsks(idx * 4 + 2)
  def askSize (idx: Int) = $bidAsks(idx * 4 + 3)

  def setBidPrice(idx: Int, v: Double) = updateDepthValue(idx * 4, v)
  def setBidSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 1, v)
  def setAskPrice(idx: Int, v: Double) = updateDepthValue(idx * 4 + 2, v)
  def setAskSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 3, v)

  def bidAsks = $bidAsks
  def bidAsks_=(that: Array[Double]) {
    isChanged = false
    if (that.length != $bidAsks.length) {
      return
    }
    var i = 0
    val length = that.length
    while (i < length) {
      updateDepthValue(i, that(i))
      i += 1
    }
  }

  private def updateDepthValue(idx: Int, v: Double) {
    if ($bidAsks(idx) != v) {
      isChanged = true
    }
    $bidAsks(idx) = v
  }

}