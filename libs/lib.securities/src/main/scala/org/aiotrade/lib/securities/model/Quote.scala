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
import org.aiotrade.lib.math.timeseries.TVal

object Quotes1d extends Quotes
object Quotes1m extends Quotes

abstract class Quotes extends Table[Quote] {
  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val open   = "open"   FLOAT(12, 2)
  val high   = "high"   FLOAT(12, 2)
  val low    = "low"    FLOAT(12, 2)
  val close  = "close"  FLOAT(12, 2)
  val volume = "volume" FLOAT(12, 2)
  val amount = "amount" FLOAT(12, 2)
  val vwap   = "vwap"   FLOAT(12, 2)

  val adjWeight = "adjWeight" FLOAT(12, 2)

  val flag = "flag" INTEGER

  // Foreign keys
  def tickers = inverse(Tickers.quote)
  def fillRecords = inverse(FillRecords.quote)

  INDEX("time_idx", time.name)

  def closedQuotesOf(sec: Sec): Seq[Quote] = {
    (SELECT (this.*) FROM (this) WHERE ((Quotes.this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Quote.MaskClosed) EQ 1)) ORDER_BY (this.time) list)
  }

  def evictCacheOfClosedQuotes(quotes: Array[Quote]) {
    var i = 0
    while (i < quotes.length) {
      val quote = quotes(i)
      if (quote.closed_?) {
        evictCache(quote)
      }
    }
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
  private val ADJWEIGHT = 7

  // bit masks for flag
  val MaskClosed   = 0x01   // 1   2^^0    000...00000001
  val MaskVerified = 0x02   // 2   2^^1    000...00000010
  private val flagbit3     = 0x04   // 4   2^^2    000...00000100
  private val flagbit4     = 0x08   // 8   2^^3    000...00001000
  private val flagbit5     = 0x10   // 16  2^^4    000...00010000
  private val flagbit6     = 0x20   // 32  2^^5    000...00100000
  private val flagbit7     = 0x40   // 64  2^^6    000...01000000
  private val flagBit8     = 0x80   // 128 2^^7    000...10000000
}

import Quote._
@serializable
class Quote extends TVal {
  var sec: Sec = _
  
  private val data = new Array[Float](8)

  @transient var sourceId = 0L

  var hasGaps = false

  def open      = data(OPEN)
  def high      = data(HIGH)
  def low       = data(LOW)
  def close     = data(CLOSE)
  def volume    = data(VOLUME)
  def amount    = data(AMOUNT)
  def vwap      = data(VWAP)
  def adjWeight = data(ADJWEIGHT)

  def open_=     (v: Float) = data(OPEN)      = v
  def high_=     (v: Float) = data(HIGH)      = v
  def low_=      (v: Float) = data(LOW)       = v
  def close_=    (v: Float) = data(CLOSE)     = v
  def volume_=   (v: Float) = data(VOLUME)    = v
  def amount_=   (v: Float) = data(AMOUNT)    = v
  def vwap_=     (v: Float) = data(VWAP)      = v
  def adjWeight_=(v: Float) = data(ADJWEIGHT) = v

  var flag: Int = 1 // dafault is closed

  // Foreign keys
  var tickers: List[Ticker] = Nil
  var fillRecords: List[FillRecord] = Nil

  def closed_? = (flag & MaskClosed) == 1
  def closed_! {
    flag |= MaskClosed
  }
  def unclosed_! {
    flag &= ~MaskClosed
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
