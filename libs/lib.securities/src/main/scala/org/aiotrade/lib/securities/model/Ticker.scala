package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.util.actors.Event
import ru.circumflex.orm.Table

/**
 * Assume table BidAsk's structure:
 val idx = intColumn("idx")
 val isBid = booleanColumn("isBid")
 val price = numericColumn("price",  12, 2)
 val size = numericColumn("size", 12, 2)

 // Select latest ask_bid in each group of "isBid" and "idx"
 def latest = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)"
 }

 def latest(intraDayId: Long) = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay_id = " + intraDayId + ") AND intraDay_id = " + intraDayId
 }

 */

object Tickers extends Table[Ticker] {
  val quote = "quotes_id" REFERENCES(Quotes1d)

  val time = "time" BIGINT

  val prevClose = "prevClose" FLOAT(12, 2)
  val lastPrice = "lastPrice" FLOAT(12, 2)

  val dayOpen   = "dayOprn"   FLOAT(12, 2)
  val dayHigh   = "dayHigh"   FLOAT(12, 2)
  val dayLow    = "dayLow"    FLOAT(12, 2)
  val dayVolume = "dayVolume" FLOAT(12, 2)
  val dayAmount = "dayAmount" FLOAT(12, 2)

  val dayChange = "dayChange" FLOAT(12, 2)

  val bidAsks = "bidAsks" SERIALIZED(classOf[Array[Float]], 200)

  INDEX("time_idx", time.name)
}

case class TickerEvent (source: Sec, ticker: Ticker) extends Event
case class TickersEvent(source: Sec, ticker: List[Ticker]) extends Event

/**
 *
 * This is just a lightweight value object. So, it can be used to lightly store
 * tickers at various time. That is, you can store many many tickers for same
 * symbol efficiently, as in case of composing an one minute ser.
 *
 * The TickerSnapshot will present the last current snapshot ticker for one
 * symbol, and implement Observable. You only need one TickerSnapshot for each
 * symbol.
 *
 * @author Caoyuan Deng
 */
@serializable @cloneable
class Ticker(val depth: Int) extends LightTicker {
  @transient final protected var isChanged: Boolean = _

  /**
   * 0 - bid price
   * 1 - bid size
   * 2 - ask price
   * 3 - ask size
   */
  var bidAsks = new Array[Float](depth * 4)

  def this() = this(5)

  override protected def updateFieldValue(fieldIdx: Int, v: Float): Boolean = {
    isChanged = super.updateFieldValue(fieldIdx, v)
    isChanged
  }

  final def bidPrice(idx: Int) = bidAsks(idx * 4)
  final def bidSize (idx: Int) = bidAsks(idx * 4 + 1)
  final def askPrice(idx: Int) = bidAsks(idx * 4 + 2)
  final def askSize (idx: Int) = bidAsks(idx * 4 + 3)

  final def setBidPrice(idx: Int, v: Float) = updateDepthValue(idx * 4, v)
  final def setBidSize (idx: Int, v: Float) = updateDepthValue(idx * 4 + 1, v)
  final def setAskPrice(idx: Int, v: Float) = updateDepthValue(idx * 4 + 2, v)
  final def setAskSize (idx: Int, v: Float) = updateDepthValue(idx * 4 + 3, v)

  private def updateDepthValue(idx: Int, v: Float) {
    isChanged = bidAsks(idx) != v
    bidAsks(idx) = v
  }

  override def reset {
    super.reset

    var i = 0
    while (i < bidAsks.length) {
      bidAsks(i) = 0
      i += 1
    }
  }

  def copyFrom(another: Ticker) = {
    super.copyFrom(another)
    System.arraycopy(another.bidAsks, 0, bidAsks, 0, bidAsks.length)
  }

  final def isValueChanged(another: Ticker): Boolean = {
    if (super.isValueChanged(another)) {
      return true
    }

    var i = 0
    while (i < bidAsks.length) {
      if (bidAsks(i) != another.bidAsks(i)) {
        return true
      }

      i += 1
    }

    false
  }

  final def isDayVolumeGrown(prevTicker: Ticker): Boolean = {
    dayVolume > prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isDayVolumeChanged(prevTicker: Ticker): Boolean = {
    dayVolume != prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isSameDay(prevTicker: Ticker, cal: Calendar): Boolean = {
    cal.setTimeInMillis(time)
    val monthA = cal.get(Calendar.MONTH)
    val dayA = cal.get(Calendar.DAY_OF_MONTH)
    cal.setTimeInMillis(prevTicker.time)
    val monthB = cal.get(Calendar.MONTH)
    val dayB = cal.get(Calendar.DAY_OF_MONTH)

    monthB == monthB && dayA == dayB
  }

  final def changeInPercent: Float = {
    if (prevClose == 0) 0f  else (lastPrice - prevClose) / prevClose * 100f
  }

  final def compareLastPriceTo(prevTicker: Ticker): Int = {
    if (lastPrice > prevTicker.lastPrice) 1
    else if (lastPrice == prevTicker.lastPrice) 0
    else 1
  }

  def toLightTicker: LightTicker = {
    val light = new LightTicker
    light.copyFrom(this)
    light
  }

  override def clone: Ticker = {
    try {
      return super.clone.asInstanceOf[Ticker]
    } catch {case ex: CloneNotSupportedException => ex.printStackTrace}

    null
  }
}


