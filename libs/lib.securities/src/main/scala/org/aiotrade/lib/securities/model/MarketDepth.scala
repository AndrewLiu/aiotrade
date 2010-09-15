package org.aiotrade.lib.securities.model

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
final class MarketDepth(var bidAsks: Array[Double]) {
  @transient var isChanged: Boolean = _

  def this() = this(null)

  def depth = bidAsks.length / 4
  
  def bidPrice(idx: Int) = bidAsks(idx * 4)
  def bidSize (idx: Int) = bidAsks(idx * 4 + 1)
  def askPrice(idx: Int) = bidAsks(idx * 4 + 2)
  def askSize (idx: Int) = bidAsks(idx * 4 + 3)

  def setBidPrice(idx: Int, v: Double) = updateDepthValue(idx * 4, v)
  def setBidSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 1, v)
  def setAskPrice(idx: Int, v: Double) = updateDepthValue(idx * 4 + 2, v)
  def setAskSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 3, v)

  private def updateDepthValue(idx: Int, v: Double) {
    if(bidAsks(idx) != v) {
      isChanged = true
    }
    bidAsks(idx) = v
  }

  def  setbidAsks(that : Array[Double]) {
    if(that.length != bidAsks.length) {
      return
    }
    var i =0
    val length = that.length
    while(i < length) {
      updateDepthValue(i, that(i))
      i+=1
    }
  }         
}