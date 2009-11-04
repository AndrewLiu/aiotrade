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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.securities.{QuoteItem, QuoteSer}


/**
 *
 * @author Caoyuan Deng
 */
//@IndicatorName("QUOTECOMPARE")
class QuoteCompareIndicator(baseSer: TSer) extends ContIndicator(baseSer) {
    
  private var _serToBeCompared: QuoteSer = _
        
  def serToBeCompared_=(serToBeCompared: QuoteSer): Unit = {
    this._serToBeCompared = serToBeCompared
  }
    
  val begPosition = Factor("Begin of Time Frame", 0L)
  val endPosition = Factor("End of Time Frame",   0L)
  val maxValue    = Factor("Max Value", -Float.MaxValue)
  val minValue    = Factor("Min Value", +Float.MaxValue)
    
  var open   = TVar[Float]("O", Plot.Quote)
  var high   = TVar[Float]("H", Plot.Quote)
  var low    = TVar[Float]("L", Plot.Quote)
  var close  = TVar[Float]("C", Plot.Quote)
  var volume = TVar[Float]("V", Plot.Quote)
    
  protected def computeCont(begIdx: Int, itemSize: Int): Unit = {
    /** camparing base point is the value of begin time (the most left on screen */
        
    /** always compute from most left position on screen */
    val begPos = begPosition.value.toInt//Math.min((int)begPosition.value(), begIdx);
    val endPos = endPosition.value.toInt//Math.min((int)endPosition.value(),   _dataSize - 1);
        
    /** get first value of baseSer in time frame, it will be the comparing base point */
    var baseNorm = Null.Float
    var position = begPosition.value.toInt
    var end = endPosition.value.toInt
    var break = false
    while (position <= end & !break) {
      val baseItem = _baseSer.asInstanceOf[QuoteSer].getItemByRow(position).asInstanceOf[QuoteItem]

      if (baseItem != null) {
        baseNorm = baseItem.close
        break = true
      }
            
      position += 1
    }
        
    if (baseNorm == Null.Float) {
      return
    }
        
    if (_baseSer.asInstanceOf[QuoteSer].adjusted) {
      if (!_serToBeCompared.adjusted) {
        _serToBeCompared.adjust(true)
      }
    } else {
      if (_serToBeCompared.adjusted) {
        _serToBeCompared.adjust(false)
      }
    }
        
    var compareNorm = Null.Float
    /**
     * !NOTICE
     * we only calculate this indicator's value for a timeSet showing in screen,
     * instead of all over the time frame of baseSer, thus, we use
     * this time set for loop instead of the usaully usage in other indicators:
     *        for (int i = fromIndex; i < baseItemList.size(); i++) {
     *            ....
     *        }
     *
     * Think about it, when the baseSer updated, we should re-calculate
     * all Ser instead from fromIndex.
     */
    var i = begPos
    while (i <= endPos) {
      if (i < begPosition.value) {
        /** don't calulate those is less than beginPosition to got a proper compareBeginValue */
      } else {
            
        val time = _baseSer.asInstanceOf[MasterTSer].timeOfRow(i)
            
        /**
         * !NOTICE:
         * we should fetch itemToBeCompared by time instead by position which may
         * not sync with baseSer.
         */
        _serToBeCompared.getItem(time) match {
          case null =>
          case itemToBeCompared:QuoteItem =>
            /** get first value of serToBeCompared in time frame */
            if (compareNorm == Null.Float) {
              compareNorm = itemToBeCompared.close
            }
                        
            val item = getItem(time).asInstanceOf[QuoteItem]
            if (item != null) {
              item.open  = linearAdjust(itemToBeCompared.open,  compareNorm, baseNorm)
              item.high  = linearAdjust(itemToBeCompared.high,  compareNorm, baseNorm)
              item.low   = linearAdjust(itemToBeCompared.low,   compareNorm, baseNorm)
              item.close = linearAdjust(itemToBeCompared.close, compareNorm, baseNorm)
            }
        }
      }
            
      i += 1
    }
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Float, prevNorm: Float, postNorm: Float): Float = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

}
