/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities

import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TVal

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
object Ticker {
  val PREV_CLOSE = 0
  val LAST_PRICE = 1
  val DAY_OPEN = 2
  val DAY_HIGH = 3
  val DAY_LOW = 4
  val DAY_VOLUME = 5
  val DAY_AMOUNT = 6
  val DAY_CHANGE = 7
}

@cloneable
class Ticker(val depth: Int) extends TVal {
  import Ticker._
    
  private val values = new Array[Float](8)
  private val bidPrices = new Array[Float](depth)
  private val bidSizes  = new Array[Float](depth)
  private val askPrices = new Array[Float](depth)
  private val askSizes  = new Array[Float](depth)
  private val cal = Calendar.getInstance

  def this() {
    this(5)
  }

  def bidPrice(idx: Int): Float = {
    bidPrices(idx)
  }

  def bidSize(idx: Int): Float = {
    bidSizes(idx)
  }

  def askPrice(idx: Int): Float = {
    askPrices(idx)
  }

  def askSize(idx: Int): Float = {
    askSizes(idx)
  }

  def setBidPrice(idx: Int, value:Float) {
    bidPrices(idx) = value
  }

  def setBidSize(idx: Int, value:Float) {
    bidSizes(idx) = value
  }

  def setAskPrice(idx: Int, value:Float) {
    askPrices(idx) = value
  }

  def setAskSize(idx: Int, value:Float) {
    askSizes(idx) = value
  }

  def apply(field: Int): Float = {
    values(field)
  }

  def update(field: Int, value: Float): Unit = {
    this.values(field) = value
  }

  def reset: Unit =  {
    time = 0
    for (i <- 0 until depth) {
      values(i) = 0
      bidPrices(i) = 0
      bidSizes(i) = 0
      askPrices(i) = 0
      askSizes(i) = 0
    }
  }

  def copy(another: Ticker): Unit = {
    this.time = another.time
    System.arraycopy(another.values, 0, this.values, 0, this.values.length)
    System.arraycopy(another.bidPrices, 0, this.bidPrices, 0, depth)
    System.arraycopy(another.bidSizes, 0, this.bidSizes, 0, depth)
    System.arraycopy(another.askPrices, 0, this.askPrices, 0, depth)
    System.arraycopy(another.askSizes, 0, this.askSizes, 0, depth)
  }

  def isValueChanged(another: Ticker): Boolean = {
    for (i <- 0 until values.length) {
      if (this.values(i) != another.values(i)) {
        return true
      }
    }
    for (i <- 0 until depth) {
      if (this.bidPrices(i) != another.bidPrices(i)) {
        return true
      }
    }
    for (i <- 0 until depth) {
      if (this.bidSizes(i) != another.bidSizes(i)) {
        return true
      }
    }
    for (i <- 0 until depth) {
      if (this.askPrices(i) != another.askPrices(i)) {
        return true
      }
    }
    for (i <- 0 until depth) {
      if (this.askSizes(i) != another.askSizes(i)) {
        return true
      }
    }

    return false
  }

  def isDayVolumeGrown(prevTicker: Ticker): Boolean = {
    this.values(DAY_VOLUME) > prevTicker.values(DAY_VOLUME) && isSameDay(prevTicker)
  }

  def isDayVolumeChanged(prevTicker: Ticker): Boolean = {
    this.values(DAY_VOLUME) != prevTicker.values(DAY_VOLUME) && isSameDay(prevTicker)
  }

  def isSameDay(prevTicker: Ticker): Boolean = {
    cal.setTimeInMillis(time)
    val month0 = cal.get(Calendar.MONTH)
    val day0 = cal.get(Calendar.DAY_OF_MONTH)
    cal.setTimeInMillis(prevTicker.time)
    val month1 = cal.get(Calendar.MONTH)
    val day1 = cal.get(Calendar.DAY_OF_MONTH)
        
    month1 == month1 && day0 == day1
  }

  def changeInPercent: Float = {
    if (values(PREV_CLOSE) == 0) 0f  else (values(LAST_PRICE) - values(PREV_CLOSE)) / values(PREV_CLOSE) * 100f
  }

  def compareLastCloseTo(prevTicker: Ticker) : Int = {
    if (values(LAST_PRICE) > prevTicker.values(LAST_PRICE)) 1
    else {
      if (values(LAST_PRICE) == prevTicker.values(LAST_PRICE)) 0 else 1
    }
  }

  override def clone: Ticker = {
    try {
      return super.clone.asInstanceOf[Ticker]
    } catch {case ex: CloneNotSupportedException => ex.printStackTrace}
        
    null
  }
}
