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

import org.aiotrade.lib.math.timeseries.{DefaultItem,TimeValue}

/**
 * QuoteItem class
 * 
 * @author Caoyuan Deng
 */
class QuoteItem(_ser:QuoteSer, time:Long) extends DefaultItem(_ser, time) {

  override def ser :QuoteSer = super.ser.asInstanceOf[QuoteSer]
    
  def volume :Float = getFloat(ser.volume)
    
  def open :Float = getFloat(ser.open)
    
  def high :Float = getFloat(ser.high)
    
  def low :Float = getFloat(ser.low)
    
  def close : Float = getFloat(ser.close)
    
  def close_adj :Float = getFloat(ser.close_adj)
    
  def close_ori :Float = getFloat(ser.close_ori)
    
  def open_=(open:Float) :Unit = setFloat(ser.open, open)

  def high_=(high:Float) :Unit = setFloat(ser.high, high)
    
  def low_=(low:Float) :Unit = setFloat(ser.low, low)
    
  def close_=(close:Float) :Unit = setFloat(ser.close, close)
    
  def volume_=(volume:Float) = setFloat(ser.volume, volume)
    
  def close_ori_=(close_ori:Float) = setFloat(ser.close_ori, close_ori)
    
  def close_adj_=(close_adj:Float) =setFloat(ser.close_adj, close_adj)

  override def assignValue[T <: TimeValue](value:T) :Unit = value match {
    case quote:Quote =>
      open   = quote.open
      high   = quote.high
      low    = quote.low
      close  = quote.close
      volume = quote.volume

      close_ori = quote.close

      val adjuestedClose = if (quote.close_adj != 0 ) quote.close_adj else quote.close
      close_adj = adjuestedClose
    case _ => assert(false, "Should pass a Quote type TimeValue")
  }

}