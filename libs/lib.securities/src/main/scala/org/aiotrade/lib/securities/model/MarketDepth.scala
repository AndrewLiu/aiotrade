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
final class MarketDepth(_bidAsks: Array[Double]) {
  @transient var isChanged: Boolean = _

  def this() = this(null)

  def depth = _bidAsks.length / 4
  
  def bidPrice(idx: Int) = _bidAsks(idx * 4)
  def bidSize (idx: Int) = _bidAsks(idx * 4 + 1)
  def askPrice(idx: Int) = _bidAsks(idx * 4 + 2)
  def askSize (idx: Int) = _bidAsks(idx * 4 + 3)

  def setBidPrice(idx: Int, v: Double) = updateDepthValue(idx * 4, v)
  def setBidSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 1, v)
  def setAskPrice(idx: Int, v: Double) = updateDepthValue(idx * 4 + 2, v)
  def setAskSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 3, v)

  def bidAsks = _bidAsks
  def bidAsks_=(that: Array[Double]) {
    isChanged = false
    if (that.length != _bidAsks.length) {
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
    if (_bidAsks(idx) != v) {
      isChanged = true
    }
    _bidAsks(idx) = v
  }

}