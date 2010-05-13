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

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.json.JsonOutputStreamWriter
import org.aiotrade.lib.json.JsonSerializable

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

private object LightTickerConstants {
  val PREV_CLOSE = 0
  val CURR_PRICE = 1
  val DAY_OPEN   = 2
  val DAY_HIGH   = 3
  val DAY_LOW    = 4
  val DAY_VOLUME = 5
  val DAY_AMOUNT = 6
  val DAY_CHANGE = 7

  val FIELD_LENGTH = 8
}

import LightTickerConstants._
@cloneable @serializable @SerialVersionUID(1L)
class LightTicker extends TVal with JsonSerializable {
  var quote: Quote = _

  final var symbol: String = _

  private val data = new Array[Float](FIELD_LENGTH)

  final def prevClose = data(PREV_CLOSE)
  final def currPrice = data(CURR_PRICE)
  final def dayOpen   = data(DAY_OPEN)
  final def dayHigh   = data(DAY_HIGH)
  final def dayLow    = data(DAY_LOW)
  final def dayVolume = data(DAY_VOLUME)
  final def dayAmount = data(DAY_AMOUNT)
  final def dayChange = data(DAY_CHANGE)

  final def prevClose_=(v: Float) = updateFieldValue(PREV_CLOSE, v)
  final def currPrice_=(v: Float) = updateFieldValue(CURR_PRICE, v)
  final def dayOpen_=  (v: Float) = updateFieldValue(DAY_OPEN,   v)
  final def dayHigh_=  (v: Float) = updateFieldValue(DAY_HIGH,   v)
  final def dayLow_=   (v: Float) = updateFieldValue(DAY_LOW,    v)
  final def dayVolume_=(v: Float) = updateFieldValue(DAY_VOLUME, v)
  final def dayAmount_=(v: Float) = updateFieldValue(DAY_AMOUNT, v)
  final def dayChange_=(v: Float) = updateFieldValue(DAY_CHANGE, v)

  protected def updateFieldValue(fieldIdx: Int, v: Float): Boolean = {
    val isChanged = data(fieldIdx) != v
    data(fieldIdx) = v
    isChanged
  }

  def reset {
    time = 0

    var i = 0
    while (i < data.length) {
      data(i) = 0
      i += 1
    }
  }

  def copyFrom(another: LightTicker) {
    this.time   = another.time
    this.symbol = another.symbol
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  def isValueChanged(another: LightTicker): Boolean = {
    var i = 0
    while (i < data.length) {
      if (data(i) != another.data(i)) {
        return true
      }
      i += 1
    }

    false
  }

  override def clone: LightTicker = {
    val cloneOne = new LightTicker
    cloneOne.copyFrom(this)
    cloneOne
  }

  @throws(classOf[IOException])
  def writeJson(out: JsonOutputStreamWriter) {
    out.write("s", symbol)
    out.write(',')
    out.write("t", time / 1000)
    out.write(',')
    out.write("v", data)
  }

  @throws(classOf[IOException])
  def readJson(fields: Map[String, _]) {
    symbol  = fields("s").asInstanceOf[String]
    time    = fields("t").asInstanceOf[Long] * 1000
    var vs  = fields("v").asInstanceOf[List[Number]]
    var i = 0
    while (!vs.isEmpty) {
      data(i) = vs.head.floatValue
      vs = vs.tail
      i += 1
    }
  }

  override def toString = {
    val df = new SimpleDateFormat("hh:mm:ss")
    val cal = Calendar.getInstance
    cal.setTimeInMillis(time)
    symbol + ", " + df.format(cal.getTime) + ", " + data.mkString("[", ",", "]")
  }
}

