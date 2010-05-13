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

package org.aiotrade.lib.model.securities


import java.util.Calendar
import ru.circumflex.orm.Table
import org.aiotrade.lib.math.timeseries.TVal

object Quote1d extends QuoteTable
object Quote1m extends QuoteTable

abstract class QuoteTable extends Table[Quote] {
  val sec = "sec_id" REFERENCES(Sec)

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
  def tickers = inverse(Ticker.quote)
  def dealRecords = inverse(Ticker.quote)
}

/**
 * Quote value object
 *
 * @author Caoyuan Deng
 */
private object QuoteConstants {
  val OPEN      = 0
  val HIGH      = 1
  val LOW       = 2
  val CLOSE     = 3
  val VOLUME    = 4
  val AMOUNT    = 5
  val VWAP      = 6
  val ADJWEIGHT = 7
}

import QuoteConstants._
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

  /**
   * 0 means unclosed
   * > 0 means closed
   * 0000,000X closed
   * 0000,00X0 verified
   */
  var flag: Int = 0

  // Foreign keys
  var tickers: List[Ticker] = Nil
  var dealRecords: List[Ticker] = Nil

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


//class Quote {
//  var sec: Sec = _
//
//  var time: Long = _
//
//  var open:   Float = _
//  var high:   Float = _
//  var low:    Float = _
//  var close:  Float = _
//  var volume: Float = _
//  var amount: Float = _
//
//  var adjWeight: Float   = _
//
//  /**
//   * 0 means unclosed
//   * > 0 means closed
//   * 0000,000X closed
//   * 0000,00X0 verified
//   */
//  var flag: Int = 0
//
//  // Foreign keys
//  var tickers: List[Ticker] = Nil
//  var dealRecords: List[Ticker] = Nil
//}
