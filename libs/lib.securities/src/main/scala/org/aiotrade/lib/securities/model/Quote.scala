/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of AIOTrade Computing Co. nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.aiotrade.lib.securities.model


import java.util.Calendar
import ru.circumflex.orm._
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.json.JsonOutputStreamWriter
import org.aiotrade.lib.json.JsonSerializable
import scala.collection.mutable
import java.io.IOException

object Quotes1d extends Quotes {
  private val logger = Logger.getLogger(this.getClass.getSimpleName)

  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def lastDailyQuoteOf(sec: Sec): Option[Quote] = {
    (SELECT (this.*) FROM (this) WHERE (this.sec.field EQ Secs.idOf(sec)) ORDER_BY (this.time DESC) LIMIT (1) list) headOption
  }

  def dailyQuoteOf(sec: Sec, dailyRoundedTime: Long): Quote = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = mutable.Map[Sec, Quote]()
        dailyCache.put(dailyRoundedTime, map)

        (SELECT (this.*) FROM (this) WHERE (
            (this.time EQ dailyRoundedTime)
          ) list
        ) foreach {x => map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Quote
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        logger.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }

  def dailyQuoteOf_ignoreCache(sec: Sec, dailyRoundedTime: Long): Quote = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ dailyRoundedTime)
      ) list
    ) headOption match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Quote
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        logger.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }


  def dailyQuotesOf(exchange: Exchange, time: Long): Seq[Quote] = {
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)

    SELECT (Quotes1d.*) FROM (Quotes1d JOIN Secs) WHERE (
      (this.time EQ rounded) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
    ) list
  }

  def lastDailyQuotesOf(exchange: Exchange): Seq[Quote] = {
    SELECT (Quotes1d.*) FROM Quotes1d WHERE (
      Quotes1d.time EQ (
        SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
      )
    ) list
  }

}

object Quotes1m extends Quotes {
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)
  val loadDaysInMilsec = config.getInt("dataserver.loadDaysOfSer1m", 5) * 1000*60*60*24

  private val ONE_DAY = 24 * 60 * 60 * 1000

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def mintueQuotesOf(sec: Sec, dailyRoundedTime: Long): Seq[Quote] = {    
    SELECT (this.*) FROM (this) WHERE (
      this.sec.field EQ Secs.idOf(sec) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
    ) ORDER_BY (this.time DESC) list
  }

  def minuteQuoteOf(sec: Sec, minuteRoundedTime: Long): Quote = {
    if (isServer) minuteQuoteOf_oncached(sec, minuteRoundedTime) else minuteQuoteOf_cached(sec, minuteRoundedTime)
  }
  
  /**
   * @Note do not use it when table is partitioned on secs_id (for example, quotes1m on server side), since this qeury is only on time
   */
  def minuteQuoteOf_cached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = mutable.Map[Sec, Quote]()
        minuteCache.put(minuteRoundedTime, map)

        (SELECT (this.*) FROM (this) WHERE (
            (this.time EQ minuteRoundedTime)
          ) list
        ) foreach {x => map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Quote
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.ONE_MIN, newone)
        newone
    }
  }

  override def quotesOf(sec: Sec): Seq[Quote] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND
      (this.time GE System.currentTimeMillis - loadDaysInMilsec)
    ) ORDER_BY (this.time) list
  }
  

  def minuteQuoteOf_oncached(sec: Sec, minuteRoundedTime: Long): Quote = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    ) match {
      case Seq(one) =>
        one.isTransient = false
        one
      case Seq() =>
        val newone = new Quote
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.ONE_MIN, newone)
        newone
    }
  }
}

abstract class Quotes extends Table[Quote] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val open   = "open"   DOUBLE()
  val high   = "high"   DOUBLE()
  val low    = "low"    DOUBLE()
  val close  = "close"  DOUBLE()
  val volume = "volume" DOUBLE()
  val amount = "amount" DOUBLE()
  val vwap   = "vwap"   DOUBLE()

  val flag = "flag" INTEGER()

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  def quotesOf(sec: Sec): Seq[Quote] = {
    SELECT (this.*) FROM (this) WHERE (
      this.sec.field EQ Secs.idOf(sec)
    ) ORDER_BY (this.time) list
  }

  def closedQuotesOf(sec: Sec): Seq[Quote] = {
    val xs = new ArrayList[Quote]()
    for (x <- quotesOf(sec) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedQuotesOf_filterByDB(sec: Sec): Seq[Quote] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
    ) ORDER_BY (this.time) list
  }

  def saveBatch(sec: Sec, sortedQuotes: Seq[Quote]) {
    if (sortedQuotes.isEmpty) return

    val head = sortedQuotes.head
    val last = sortedQuotes.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, Quote]()
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    ) foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedQuotes.partition(x => exists.contains(x.time))
    for (x <- updates) {
      val existOne = exists(x.time)
      existOne.copyFrom(x)
      this.update_!(existOne)
    }

    this.insertBatch_!(inserts.toArray)
  }
}

/**
 * Quote value object
 *
 * @author Caoyuan Deng
 */
@serializable
class Quote extends TVal with Flag with JsonSerializable {
  @transient var _sec: Sec = _
  def sec = _sec
  def sec_=(sec: Sec) {
    _uniSymbol = sec.uniSymbol
    _sec = sec
  }
  
  private var _uniSymbol: String = _
  def uniSymbol = _uniSymbol
  
  @transient var sourceId = 0L

  var hasGaps = false
  
  private val data = new Array[Double](7)
  
  def open      = data(0)
  def high      = data(1)
  def low       = data(2)
  def close     = data(3)
  def volume    = data(4)
  def amount    = data(5)
  def vwap      = data(6)

  def open_=   (v: Double) {data(0) = v}
  def high_=   (v: Double) {data(1) = v}
  def low_=    (v: Double) {data(2) = v}
  def close_=  (v: Double) {data(3) = v}
  def volume_= (v: Double) {data(4) = v}
  def amount_= (v: Double) {data(5) = v}
  def vwap_=   (v: Double) {data(6) = v}

  // Foreign keys
  @transient var tickers: List[Ticker] = Nil
  @transient var executions: List[Execution] = Nil

  
  // --- no db fields:
  var isTransient: Boolean = true
  
  def copyFrom(another: Quote) {
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  def reset {
    time = 0
    sourceId = 0
    var i = 0
    while (i < data.length) {
      data(i) = 0
      i += 1
    }
    hasGaps = false
  }
  
  /**
   * This quote must be a daily quote
   */
  def updateDailyQuoteByTicker(ticker: Ticker) {
    open   = ticker.dayOpen
    high   = ticker.dayHigh
    low    = ticker.dayLow
    close  = ticker.lastPrice
    volume = ticker.dayVolume
    amount = ticker.dayAmount
  }

  @throws(classOf[IOException])
  def writeJson(out: JsonOutputStreamWriter) {
    out.write("s", _uniSymbol)
    out.write(',')
    out.write("t", time / 1000)
    out.write(',')
    out.write("v", data)
  }

  @throws(classOf[IOException])
  def readJson(fields: collection.Map[String, _]) {
    _uniSymbol  = fields("s").asInstanceOf[String]
    time    = fields("t").asInstanceOf[Long] * 1000
    var vs  = fields("v").asInstanceOf[List[Number]]
    var i = 0
    while (!vs.isEmpty) {
      data(i) = vs.head.doubleValue
      vs = vs.tail
      i += 1
    }
  }

  override def toString = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(time)
    
    this.getClass.getSimpleName + ": " + cal.getTime +
    " O: " + open +
    " H: " + high +
    " L: " + low +
    " C: " + close +
    " V: " + volume
  }

}
