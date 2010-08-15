package org.aiotrade.lib.securities.model

import java.util.Calendar
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

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


object TickersLast extends TickersTable
object Tickers extends TickersTable

abstract class TickersTable extends Table[Ticker] {
  private val log = Logger.getLogger(this.getClass.getName)

  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val prevClose = "prevClose" DOUBLE()
  val lastPrice = "lastPrice" DOUBLE()

  val dayOpen   = "dayOpen"   DOUBLE()
  val dayHigh   = "dayHigh"   DOUBLE()
  val dayLow    = "dayLow"    DOUBLE()
  val dayVolume = "dayVolume" DOUBLE()
  val dayAmount = "dayAmount" DOUBLE()

  val dayChange = "dayChange" DOUBLE()

  val bidAsks = "bidAsks" SERIALIZED(classOf[Array[Double]], 400)

  INDEX(getClass.getSimpleName + "_time_idx", time.name)


  private val ONE_DAY = 24 * 60 * 60 * 1000

  def lastTickerOf(sec: Sec, dailyRoundedTime: Long): Option[Ticker] = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (this.time DESC) LIMIT (1) list
    ) headOption
  }

  def tickersOf(sec: Sec, dailyRoundedTime: Long): Seq[Ticker] = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (this.time) list
    )
  }

  def lastTraingDayTickersOf(sec: Sec): Seq[Ticker] = {
    (SELECT (Tickers.time) FROM (Tickers) WHERE (
        Tickers.sec.field EQ Secs.idOf(sec)
      ) ORDER_BY (Tickers.time DESC) LIMIT (1) list
    ) headOption match {
      case Some(time) =>
        val cal = Calendar.getInstance(sec.exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)
        
        (SELECT (Tickers.*) FROM (Tickers) WHERE (
            (Tickers.sec.field EQ Secs.idOf(sec)) AND (Tickers.time BETWEEN (rounded, rounded + ONE_DAY - 1))
          ) ORDER_BY (Tickers.time) list
        )
      case None => Nil
    }
  }

  /**
   * Plan A:
   * SELECT a.* FROM orm.tickers AS a WHERE a.time = (SELECT max(time) FROM orm.tickers AS b LEFT JOIN orm.secs AS secs ON b.secs_id = secs.id WHERE b.time >= 1281715200000 AND b.time < 1281801600000 AND secs.exchanges_id = 1 AND b.secs_id = a.secs_id);
   * Plan B:
   * SELECT tickers.* FROM (SELECT tickers.secs_id as secs_id, MAX(tickers.time) AS maxtime FROM orm.tickers AS tickers LEFT JOIN orm.secs AS secs ON tickers.secs_id = secs.id WHERE tickers.time >= 1281715200000 AND tickers.time < 1281801600000 AND secs.exchanges_id = 1 GROUP BY tickers.secs_id) AS x INNER JOIN orm.tickers AS tickers ON x.secs_id = tickers.secs_id AND x.maxtime = tickers.time;
   */
  private[securities] def lastTickersOf(exchange: Exchange): HashMap[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    val start = System.currentTimeMillis
    val map = new HashMap[Sec, Ticker]
    lastTradingTimeOf(exchange) match {
      case Some(time) =>
        val cal = Calendar.getInstance(exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)

        val sql = "SELECT tickers.id AS this_1, tickers.bidAsks AS this_2, tickers.dayChange AS this_3, tickers.dayAmount AS this_4, tickers.dayVolume AS this_5, tickers.dayLow AS this_6, tickers.dayHigh AS this_7, tickers.dayOpen AS this_8, tickers.lastPrice AS this_9, tickers.prevClose AS this_10, tickers.time AS this_11, tickers.secs_id AS this_12" +
        " FROM (SELECT tickers.secs_id AS secs_id, MAX(tickers.time) AS maxtime FROM orm.tickers AS tickers LEFT JOIN orm.secs AS secs ON tickers.secs_id = secs.id" +
        " WHERE tickers.time >= " + rounded + " AND tickers.time < " + (rounded + ONE_DAY) + " AND secs.exchanges_id = " + Exchanges.idOf(exchange).get +
        " GROUP BY tickers.secs_id) AS x INNER JOIN orm.tickers AS tickers ON x.secs_id = tickers.secs_id AND x.maxtime = tickers.time;"
        
        (new Select(Tickers.*) {
            override def toSql = sql
          } list
        ) foreach {x => map.put(x.sec, x)}

        log.info("Loaded last tickers between " + rounded + " - " + (rounded + ONE_DAY - 1) +
                 " of " + exchange.code + ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
      case None =>
    }

    map
  }

  //  private[securities] def lastTickersOf(exchange: Exchange): HashMap[Sec, Ticker] = {
//    Exchange.uniSymbolToSec // force loaded all secs and secInfos
//    SELECT (Tickers.*, Quotes1d.*) FROM (Tickers JOIN (Quotes1d JOIN Secs)) WHERE (
//      (Quotes1d.time EQ (
//          SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
//        )
//      ) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
//    ) ORDER_BY (Tickers.time ASC, Tickers.id ASC) list match {
//      case xs if xs.isEmpty => new HashMap[Sec, Ticker]
//      case xs =>
//        val map = new HashMap[Sec, Ticker]
//        xs map (_._1) groupBy (_.quote.sec) foreach {case (sec, tickers) =>
//            map.put(sec, tickers.head)
//        }
//        map
//    }
//  }

  @deprecated private[securities] def lastTickersOf_depreacted(exchange: Exchange): HashMap[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    val start = System.currentTimeMillis
    val map = new HashMap[Sec, Ticker]
    lastTradingTimeOf(exchange) match {
      case Some(time) =>
        val cal = Calendar.getInstance(exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)

        (SELECT (Tickers.*) FROM (Tickers JOIN Secs) WHERE (
            (Tickers.time BETWEEN (rounded, rounded + ONE_DAY - 1)) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
          ) ORDER_BY (Tickers.time ASC) list
        ) groupBy (_.sec) foreach (xt => map.put(xt._1, xt._2.head)) // xt: (Sec, Seq[Ticker])

        log.info("Loaded last tickers between " + rounded + " to " + (rounded + ONE_DAY - 1) +
                 " of " + exchange.code + ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
      case None =>
    }

    map
  }

  private[model] def lastTradingTimeOf(exchange: Exchange): Option[Long] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    (SELECT (Tickers.time) FROM (Tickers JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange)) ORDER_BY (Tickers.time DESC) LIMIT (1) list) headOption
  }

}

object Ticker {
  def importFrom(v: (Long, List[Array[Double]])): LightTicker = v match {
    case (time: Long, List(data, bidAsks)) =>
      val x = new Ticker(data, new MarketDepth(bidAsks))
      x.time = time
      x
    case (time: Long, List(data)) =>
      val x = new LightTicker(data)
      x.time = time
      x
    case _ => null
  }
}

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
class Ticker($data: Array[Double], val marketDepth: MarketDepth) extends LightTicker($data) {

  def this(depth: Int) = this(new Array[Double](LightTicker.FIELD_LENGTH), new MarketDepth(new Array[Double](depth * 4)))
  def this() = this(5)

  def depth = marketDepth.depth

  def bidAsks = marketDepth.bidAsks
  def bidAsks_=(values: Array[Double]) {
    marketDepth.bidAsks = values
  }

  final def bidPrice(idx: Int) = marketDepth.bidPrice(idx)
  final def bidSize (idx: Int) = marketDepth.bidSize (idx)
  final def askPrice(idx: Int) = marketDepth.askPrice(idx)
  final def askSize (idx: Int) = marketDepth.askSize (idx)

  final def setBidPrice(idx: Int, v: Double) = marketDepth.setBidPrice(idx, v)
  final def setBidSize (idx: Int, v: Double) = marketDepth.setBidSize (idx, v)
  final def setAskPrice(idx: Int, v: Double) = marketDepth.setAskPrice(idx, v)
  final def setAskSize (idx: Int, v: Double) = marketDepth.setAskSize (idx, v)

  def isChanged = _isChanged || marketDepth.isChanged
  def isChanged_=(b: Boolean) = {
    _isChanged = b
  }

  override def reset {
    super.reset

    var i = 0
    while (i < bidAsks.length) {
      bidAsks(i) = 0
      i += 1
    }
  }

  override def copyFrom(another: LightTicker) {
    super.copyFrom(another)
    another match {
      case x: Ticker => System.arraycopy(x.bidAsks, 0, bidAsks, 0, bidAsks.length)
      case _ =>
    }
  }

  override def export = (time, List(data, marketDepth.bidAsks))

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


