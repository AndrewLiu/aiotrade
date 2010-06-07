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
package org.aiotrade.lib.securities

import org.aiotrade.lib.math.timeseries.{TVal, DefaultTSer, TSerEvent}
import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.securities.model.MoneyFlow

/**
 *
 * @author Caoyuan Deng
 */
class MoneyFlowSer(baseSer: QuoteSer) extends DefaultTSer(baseSer.freq) {

  attach(baseSer.timestamps)

  private var _shortDescription: String = ""
  var adjusted: Boolean = false

  val totalVolume = TVar[Float]("V", Plot.Stick)
  val totalAmount = TVar[Float]("A", Plot.None)

  val superVolume = TVar[Float]("V", Plot.None)
  val superAmount = TVar[Float]("A", Plot.None)

  val largeVolume = TVar[Float]("V", Plot.None)
  val largeAmount = TVar[Float]("A", Plot.None)

  val smallVolume = TVar[Float]("V", Plot.None)
  val smallAmount = TVar[Float]("A", Plot.None)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case mf: MoneyFlow =>
        totalVolume(time) = mf.totalVolume
        totalAmount(time) = mf.totalAmount
        superVolume(time) = mf.superVolume
        superAmount(time) = mf.superAmount
        largeVolume(time) = mf.largeVolume
        largeAmount(time) = mf.largeAmount
        smallVolume(time) = mf.smallVolume
        smallAmount(time) = mf.smallVolume

//        val adjuestedClose = if (mf.adjWeight != 0 ) mf.adjWeight else mf.close
//        close_adj(time) = adjuestedClose
      case _ =>
    }
  }

  def valueOf(time: Long): Option[MoneyFlow] = {
    if (exists(time)) {
      val mf = new MoneyFlow
      mf.totalVolume = totalVolume(time)
      mf.totalAmount = totalAmount(time)
      mf.superVolume = superVolume(time)
      mf.superAmount = superAmount(time)
      mf.largeVolume = largeVolume(time)
      mf.largeAmount = largeAmount(time)
      mf.smallVolume = smallVolume(time)
      mf.smallVolume = smallAmount(time)
      Some(mf)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(mf: MoneyFlow) {
    validate

    val time = mf.time
    totalVolume(time) = mf.totalVolume
    totalAmount(time) = mf.totalAmount
    superVolume(time) = mf.superVolume
    superAmount(time) = mf.superAmount
    largeVolume(time) = mf.largeVolume
    largeAmount(time) = mf.largeAmount
    smallVolume(time) = mf.smallVolume
    smallAmount(time) = mf.smallVolume

    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }


  /**
   * @param boolean b: if true, do adjust, else, de adjust
   */
//  def adjust(b: Boolean) {
//    var i = 0
//    while (i < size) {
//
//      var prevNorm = close(i)
//      var postNorm = if (b) {
//        /** do adjust */
//        close_adj(i)
//      } else {
//        /** de adjust */
//        close_ori(i)
//      }
//
//      high(i)  = linearAdjust(high(i),  prevNorm, postNorm)
//      low(i)   = linearAdjust(low(i),   prevNorm, postNorm)
//      open(i)  = linearAdjust(open(i),  prevNorm, postNorm)
//      close(i) = linearAdjust(close(i), prevNorm, postNorm)
//
//      i += 1
//    }
//
//    adjusted = b
//
//    val evt = TSerEvent.Updated(this, null, 0, lastOccurredTime)
//    publish(evt)
//  }
    
  /**
   * This function adjusts linear according to a norm
   */
  private def linearAdjust(value: Float, prevNorm: Float, postNorm: Float): Float = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

  override def shortDescription_=(symbol: String): Unit = {
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





