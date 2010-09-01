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
import scala.collection.mutable.HashMap

object Quotes1d extends Quotes {
  private val logger = Logger.getLogger(this.getClass.getSimpleName)

  private val dailyCache = new HashMap[Long, HashMap[Sec, Quote]]

  def lastDailyQuoteOf(sec: Sec): Option[Quote] = {
    (SELECT (this.*) FROM (this) WHERE (this.sec.field EQ Secs.idOf(sec)) ORDER_BY (this.time DESC) LIMIT (1) list) headOption
  }

  def dailyQuoteOf(sec: Sec, dailyRoundedTime: Long): Quote = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = new HashMap[Sec, Quote]
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
        logger.info("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewDailyQuote(newone)
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
        logger.info("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewDailyQuote(newone)
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
  private val ONE_DAY = 24 * 60 * 60 * 1000

  private val minuteCache = new HashMap[Long, HashMap[Sec, Quote]]

  def mintueQuotesOf(sec: Sec, dailyRoundedTime: Long): Seq[Quote] = {    
    SELECT (this.*) FROM (this) WHERE (
      this.sec.field EQ Secs.idOf(sec) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
    ) ORDER_BY (this.time DESC) list
  }

  // Since Quotes1m is partitioned into secs_id, don't query it only on time
  @deprecated def minuteQuoteOf_cached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = new HashMap[Sec, Quote]
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
        newone
    }
  }

  def minuteQuoteOf(sec: Sec, minuteRoundedTime: Long): Quote = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    ) headOption match {
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
        newone
    }
  }
}

abstract class Quotes extends Table[Quote] {
  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val open   = "open"   DOUBLE()
  val high   = "high"   DOUBLE()
  val low    = "low"    DOUBLE()
  val close  = "close"  DOUBLE()
  val volume = "volume" DOUBLE()
  val amount = "amount" DOUBLE()
  val vwap   = "vwap"   DOUBLE()

  val flag = "flag" INTEGER

  INDEX(getClass.getSimpleName + "_time_idx", time.name)

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
    val exists = new HashMap[Long, Quote]
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
object Quote {
  private val OPEN      = 0
  private val HIGH      = 1
  private val LOW       = 2
  private val CLOSE     = 3
  private val VOLUME    = 4
  private val AMOUNT    = 5
  private val VWAP      = 6
}

import Quote._
@serializable
class Quote extends TVal with Flag {
  var sec: Sec = _
  
  private val data = new Array[Double](7)

  @transient var sourceId = 0L

  var hasGaps = false

  def open      = data(OPEN)
  def high      = data(HIGH)
  def low       = data(LOW)
  def close     = data(CLOSE)
  def volume    = data(VOLUME)
  def amount    = data(AMOUNT)
  def vwap      = data(VWAP)

  def open_=     (v: Double) {data(OPEN)      = v}
  def high_=     (v: Double) {data(HIGH)      = v}
  def low_=      (v: Double) {data(LOW)       = v}
  def close_=    (v: Double) {data(CLOSE)     = v}
  def volume_=   (v: Double) {data(VOLUME)    = v}
  def amount_=   (v: Double) {data(AMOUNT)    = v}
  def vwap_=     (v: Double) {data(VWAP)      = v}

  // Foreign keys
  var tickers: List[Ticker] = Nil
  var executions: List[Execution] = Nil

  
  // --- no db fields:
  var isTransient: Boolean = true
  
  def copyFrom(another: Quote) {
    var i = 0
    while (i < data.length) {
      data(i) = another.data(i)
      i += 1
    }
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

  override def toString = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(time)
    this.getClass.getSimpleName + ": " + cal.getTime +
    " O: " + data(OPEN) +
    " H: " + data(HIGH) +
    " L: " + data(LOW) +
    " C: " + data(CLOSE) +
    " V: " + data(VOLUME)
  }
}
