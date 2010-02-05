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
 * Quote value object
 *
 * @author Caoyuan Deng
 */
@serializable
class Quote extends TVal {
    
  private val OPEN      = 0
  private val HIGH      = 1
  private val LOW       = 2
  private val CLOSE     = 3
  private val VOLUME    = 4
  private val AMOUNT    = 5
  private val CLOSE_ADJ = 6
  private val VWAP       = 7
    
  private val values = new Array[Float](8)
    
  var sourceId: Long = _
    
  var hasGaps: Boolean = false
        
  def open:   Float = values(OPEN)
  def high:   Float = values(HIGH)
  def low:    Float = values(LOW)
  def close:  Float = values(CLOSE)
  def volume: Float = values(VOLUME)
  def amount: Float = values(AMOUNT)
  def close_adj: Float = values(CLOSE_ADJ)
  def vwap: Float = values(VWAP)

  def open_=  (open:   Float) = values(OPEN)   = open
  def high_=  (high:   Float) = values(HIGH)   = high
  def low_=   (low:    Float) = values(LOW)    = low
  def close_= (close:  Float) = values(CLOSE)  = close
  def volume_=(volume: Float) = values(VOLUME) = volume
  def amount_=(amount: Float) = values(AMOUNT) = amount
  def close_adj_=(close_adj: Float) = values(CLOSE_ADJ) = close_adj
  def vwap_=(wap: Float) = values(VWAP) = wap

  def reset {
    time = 0
    sourceId = 0
    var i = 0
    while (i < values.length) {
      values(i) = 0
      i += 1
    }
    hasGaps = false
  }

  override def toString = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(time)
    this.getClass.getSimpleName + ": " + cal.getTime +
    " O: " + values(OPEN) +
    " H: " + values(HIGH) +
    " L: " + values(LOW) +
    " C: " + values(CLOSE) +
    " V: " + values(VOLUME)
  }
}
