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

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Sec

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
    
  private var _shortDescription: String = ""
  var adjusted: Boolean = false
    
  val open   = TVar[Double]("O", Plot.Quote)
  val high   = TVar[Double]("H", Plot.Quote)
  val low    = TVar[Double]("L", Plot.Quote)
  val close  = TVar[Double]("C", Plot.Quote)
  val volume = TVar[Double]("V", Plot.Volume)
  val amount = TVar[Double]("A", Plot.Volume)
    
  val close_adj = TVar[Double]("W")
  val close_ori = TVar[Double]()

  val isClosed = TVar[Boolean]()

  override def serProvider: Sec = super.serProvider.asInstanceOf[Sec]

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case quote: Quote =>
        open(time)   = quote.open
        high(time)   = quote.high
        low(time)    = quote.low
        close(time)  = quote.close
        volume(time) = quote.volume
        amount(time) = quote.amount

        close_ori(time) = quote.close

        val adjuestedClose = /* if (quote.adjWeight != 0 ) quote.adjWeight else */ quote.close
        close_adj(time) = adjuestedClose
        
        isClosed(time) = quote.closed_?
      case _ => assert(false, "Should pass a Quote type TimeValue")
    }
  }

  def valueOf(time: Long): Option[Quote] = {
    if (exists(time)) {
      val quote = new Quote
      quote.open   = open(time)
      quote.high   = high(time)
      quote.low    = low(time)
      quote.close  = close(time)
      quote.volume = volume(time)
      quote.amount = amount(time)
      Some(quote)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(quote: Quote) {
    val time = quote.time
    createOrClear(time)

    open(time)   = quote.open
    high(time)   = quote.high
    low(time)    = quote.low
    close(time)  = quote.close
    volume(time) = quote.volume
    amount(time) = quote.amount

    close_ori(time) = quote.close
    close_adj(time) = quote.close

    isClosed(time) = quote.closed_?

    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }


  /**
   * @param boolean b: if true, do adjust, else, de adjust
   */
  def adjust(b: Boolean) {
    var i = 0
    while (i < size) {
            
      var prevNorm = close(i)
      var postNorm = if (b) {
        /** do adjust */
        close_adj(i)
      } else {
        /** de adjust */
        close_ori(i)
      }
                        
      high(i)  = linearAdjust(high(i),  prevNorm, postNorm)
      low(i)   = linearAdjust(low(i),   prevNorm, postNorm)
      open(i)  = linearAdjust(open(i),  prevNorm, postNorm)
      close(i) = linearAdjust(close(i), prevNorm, postNorm)

      i += 1
    }
        
    adjusted = b
        
    val evt = TSerEvent.Updated(this, null, 0, lastOccurredTime)
    publish(evt)
  }
    
  /**
   * This function adjusts linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

  override def shortDescription_=(symbol: String) {
    this._shortDescription = symbol
  }
    
  override def shortDescription: String = {
    if (adjusted) {
      _shortDescription + "(*)"
    } else {
      _shortDescription
    }
  }
    
}





