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
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.util
import scala.collection.mutable



/**
 * Quote value object
 *
 * @author Caoyuan Deng
 */
@serializable
class Quote extends BelongsToSec with TVal with Flag {

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag 
  def flag_=(flag: Int) {
    this._flag = flag
  }

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
    var i = -1
    while ({i += 1; i < data.length}) {
      data(i) = 0
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

  override def toString = {
    val sb = new StringBuilder()
    sb.append("Quote(").append(util.formatTime(time))
    sb.append(",O:").append(open)
    sb.append(",H:").append(high)
    sb.append(",L:").append(low)
    sb.append(",C:").append(close)
    sb.append(",V:").append(volume)
    sb.append(")").toString
  }

}

// --- table
abstract class Quotes extends Table[Quote] {
  private val log = Logger.getLogger(this.getClass.getName)
  
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
    try {
      SELECT (this.*) FROM (this) WHERE (
        this.sec.field EQ Secs.idOf(sec)
      ) ORDER_BY (this.time) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def closedQuotesOf(sec: Sec): Seq[Quote] = {
    val xs = new ArrayList[Quote]()
    for (x <- quotesOf(sec) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedQuotesOf_filterByDB(sec: Sec): Seq[Quote] = {
    try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
      ) ORDER_BY (this.time) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def saveBatch(sec: Sec, sortedQuotes: Seq[Quote]) {
    if (sortedQuotes.isEmpty) return

    val head = sortedQuotes.head
    val last = sortedQuotes.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, Quote]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedQuotes.partition(x => exists.contains(x.time))
    try {
      for (x <- updates) {
        val existOne = exists(x.time)
        existOne.copyFrom(x)
        this.update_!(existOne)
      }

      this.insertBatch_!(inserts.toArray)
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
  
  def saveBatch(atSameTime: Long, quotes: Array[Quote]) {
    if (quotes.isEmpty) return

    val exists = mutable.Map[Sec, Quote]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sec.field GT 0) AND (this.sec.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.sec, x)}

    val updates = new ArrayList[Quote]()
    val inserts = new ArrayList[Quote]()
    var i = -1
    while ({i += 1; i < quotes.length}) {
      val quote = quotes(i)
      exists.get(quote.sec) match {
        case Some(existOne) => 
          existOne.copyFrom(quote)
          updates += existOne
        case None =>
          inserts += quote
      }
    }
    
    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
}

object Quotes1d extends Quotes {
  private val log = Logger.getLogger(this.getClass.getSimpleName)

  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def lastDailyQuoteOf(sec: Sec): Option[Quote] = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (this.sec.field EQ Secs.idOf(sec)) ORDER_BY (this.time DESC) LIMIT (1) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res.headOption
  }

  @deprecated
  def dailyQuoteOf(sec: Sec, dailyRoundedTime: Long): Quote = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = mutable.Map[Sec, Quote]()
        dailyCache.put(dailyRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ dailyRoundedTime)
          ) list
        } catch {
          case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x => map.put(x.sec, x)}

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
        log.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }

  @deprecated
  def dailyQuoteOf_nonCached(sec: Sec, dailyRoundedTime: Long): Quote = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ dailyRoundedTime)
      ) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res.headOption match {
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
        log.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }


  def dailyQuotesOf(exchange: Exchange, time: Long): Seq[Quote] = {
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)

    try {
      SELECT (Quotes1d.*) FROM (Quotes1d JOIN Secs) WHERE (
        (this.time EQ rounded) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
      ) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def lastDailyQuotesOf(exchange: Exchange): Seq[Quote] = {
    try {
      SELECT (Quotes1d.*) FROM Quotes1d WHERE (
        Quotes1d.time EQ (
          SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
        )
      ) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

}

object Quotes1m extends Quotes {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)

  private val ONE_DAY = 24 * 60 * 60 * 1000

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def mintueQuotesOf(sec: Sec, dailyRoundedTime: Long): Seq[Quote] = {    
    try {
      SELECT (this.*) FROM (this) WHERE (
        this.sec.field EQ Secs.idOf(sec) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (this.time DESC) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  @deprecated
  def minuteQuoteOf(sec: Sec, minuteRoundedTime: Long): Quote = {
    if (isServer) minuteQuoteOf_nonCached(sec, minuteRoundedTime) else minuteQuoteOf_cached(sec, minuteRoundedTime)
  }
  
  /**
   * @Note do not use it when table is partitioned on secs_id (for example, quotes1m on server side), since this qeury is only on time
   */
  @deprecated
  def minuteQuoteOf_cached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = mutable.Map[Sec, Quote]()
        minuteCache.put(minuteRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ minuteRoundedTime)
          ) list
        } catch {
          case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x => map.put(x.sec, x)}

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

  @deprecated
  def minuteQuoteOf_nonCached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res match {
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
