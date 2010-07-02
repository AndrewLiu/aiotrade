package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.util.actors.Event
import ru.circumflex.orm.Table
import ru.circumflex.orm._

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

  INDEX(getClass.getSimpleName + "_time_idx", time.name)

  def lastTickerOf(dailyQuote: Quote): Option[Ticker] = {
    (SELECT (this.*) FROM (this) WHERE (this.quote.field EQ Quotes1d.idOf(dailyQuote)) ORDER_BY (this.time DESC, this.id DESC) LIMIT (1) list) headOption
  }

  def tickersOf(dailyQuote: Quote): Seq[Ticker] = {
    (SELECT (this.*) FROM (this) WHERE (this.quote.field EQ Quotes1d.idOf(dailyQuote)) ORDER_BY (this.time) list)
  }

  def lastTickersOf(exchange: Exchange): Seq[Ticker] = {
    SELECT (Tickers.*, Quotes1d.*) FROM (Tickers JOIN (Quotes1d JOIN Secs)) WHERE (
      (Quotes1d.time EQ (
          SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
        )
      ) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
    ) ORDER_BY (Tickers.time ASC, Tickers.id ASC) list match {
      case xs if xs.isEmpty => Nil
      case xs => xs map (_._1) groupBy (_.quote) map (_._2.head) toSeq
    }
  }

  def lastTickersSql = {
    /* (SELECT (Tickers.*) FROM (
     (SELECT (Tickers.quotes_id, MAX(time) AS maxtime) FROM (tickers) GROUP_BY Tickers.quotes_id) AS x INNER_JOIN Tickers ON (
     (Tickers.quotes_id EQ x.quotes_id) AND (Tickers.time = x.maxtime))
     ) list
     ) */
    val sql =
      "SELECT a.quotes_id, a.time FROM (SELECT quotes_id, MAX(time) AS maxtime FROM tickers GROUP BY quotes_id) AS x INNER JOIN tickers as a ON a.quotes_id = x.quotes_id AND a.time = x.maxtime;"
  }
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
  @transient final protected var _isChanged: Boolean = _

  val marketDepth = new MarketDepth(new Array[Float](depth * 4))

  def this() = this(5)

  def bidAsks = marketDepth.bidAsks
  def bidAsks_=(values: Array[Float]) {
    marketDepth.bidAsks = values
  }

  final def bidPrice(idx: Int) = marketDepth.bidPrice(idx)
  final def bidSize (idx: Int) = marketDepth.bidSize (idx)
  final def askPrice(idx: Int) = marketDepth.askPrice(idx)
  final def askSize (idx: Int) = marketDepth.askSize (idx)

  final def setBidPrice(idx: Int, v: Float) = marketDepth.setBidPrice(idx, v)
  final def setBidSize (idx: Int, v: Float) = marketDepth.setBidSize (idx, v)
  final def setAskPrice(idx: Int, v: Float) = marketDepth.setAskPrice(idx, v)
  final def setAskSize (idx: Int, v: Float) = marketDepth.setAskSize (idx, v)

  def isChanged = _isChanged || marketDepth.isChanged
  def isChanged_=(b: Boolean) = {
    _isChanged = b
  }

  override protected def updateFieldValue(fieldIdx: Int, v: Float): Boolean = {
    _isChanged = super.updateFieldValue(fieldIdx, v)
    _isChanged
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


