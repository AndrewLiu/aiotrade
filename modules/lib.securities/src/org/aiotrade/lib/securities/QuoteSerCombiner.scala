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
import java.util.TimeZone
import org.aiotrade.lib.math.timeseries.{SerChangeEvent,SerChangeListener,TUnit}

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSerCombiner(sourceQuoteSer: QuoteSer, targetQuoteSer: QuoteSer, timeZone: TimeZone) {

  private val cal = Calendar.getInstance(timeZone)

  private val sourceSerChangelistener = new SerChangeListener {
    def serChanged(evt: SerChangeEvent): Unit = {
      if (evt.tpe == SerChangeEvent.Type.FinishedComputing) {
        computeCont(evt.beginTime)
      }
    }
  }
        
  sourceQuoteSer.addSerChangeListener(sourceSerChangelistener)
    
  def computeFrom(fromTime: Long): Unit = {
    computeCont(fromTime)
  }
    
  /**
   * Combine data according to wanted frequency, such as Weekly, Monthly etc.
   */
  protected def computeCont(fromTime: Long): Unit = {
    val targetUnit = targetQuoteSer.freq.unit
        
    val masterFromTime = targetUnit.beginTimeOfUnitThatInclude(fromTime, cal)
    val masterFromIdx1 = sourceQuoteSer.timestamps.indexOfNearestOccurredTimeBehind(masterFromTime)
    val masterFromIdx = if (masterFromIdx1 < 0) 0 else masterFromIdx1
        
    //System.out.println("myFromIdx: " + myFromIdx + " materFromIdx: " + masterFromIdx);
    //targetQuoteSer.clear(myFromTime);
        
    /** begin combining: */
                
    val sourceItems = sourceQuoteSer.items
        
    val size = sourceItems.size
    //for (i <- masterFromIdx until size) {
    def loop(i: Int): Unit = {
      if (i >= size) return
            
      val item_i = sourceItems(i).asInstanceOf[QuoteItem]
            
      val time_i = item_i.time
            
      if (time_i < masterFromTime) {
        loop(i + 1)
      }
            
      val intervalBegin = targetUnit.beginTimeOfUnitThatInclude(time_i, cal)
            
      cal.setTimeInMillis(intervalBegin)
      val currWeekOfYear  = cal.get(Calendar.WEEK_OF_YEAR)
      val currMonthOfYear = cal.get(Calendar.MONTH)
      val currYear        = cal.get(Calendar.YEAR)
            
      val taritemOf = targetQuoteSer.createItemOrClearIt(intervalBegin).asInstanceOf[QuoteItem]
            
      var prevNorm = item_i.close
      var postNorm = item_i.close_adj
            
      taritemOf.open   = linearAdjust(item_i.open,  prevNorm, postNorm)
      taritemOf.high   = -Float.MaxValue
      taritemOf.low    = +Float.MaxValue
      taritemOf.volume = 0
            
      /** compose followed source data of this interval to targetData */
      var j = 0
      var break = false
      while (i + j < size && !break) {
        val item_j = sourceItems(i + j).asInstanceOf[QuoteItem]
        val time_j = item_j.time
                
        cal.setTimeInMillis(time_j)
                
        var inSameInterval = true
        targetUnit match {
          case TUnit.Week =>
            val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
            inSameInterval = (weekOfYear == currWeekOfYear)
          case TUnit.Month =>
            val monthOfYear = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            inSameInterval = (year == currYear && monthOfYear == currMonthOfYear)
          case _ =>
        }
                
        /**
         * @NOTICE
         * There is strange behave of JDK's Calendar when a very big Long
         * timeInMillis assigned to it. so anyway, we let inSamePeriod = true
         * if j == 0, because jdata is the same data as idata in this case:
         */
        inSameInterval = if (j == 0) true else inSameInterval
                
        if (inSameInterval) {
          /**
           * @TIPS
           * when combine, do adjust on source's value, then de adjust on combined quote data.
           * this will prevent bad high, open, and low into combined quote data:
           *
           * Duaring the combining period, an adjust may happened, but we only record last
           * close_adj, the high, low, and open of the data before adjusted acutally may has
           * different scale close_adj, so must do adjust with its own close_adj firstly. then
           * use the last close_orj to de-adjust it.
           */
          prevNorm = item_j.close
          postNorm = item_j.close_adj

          taritemOf.high  = Math.max(taritemOf.high, linearAdjust(item_j.high,  prevNorm, postNorm))
          taritemOf.low   = Math.min(taritemOf.low,  linearAdjust(item_j.low,   prevNorm, postNorm))
          taritemOf.close = linearAdjust(item_j.close,   prevNorm, postNorm)

          taritemOf.volume = taritemOf.volume + item_j.volume

          taritemOf.close_ori = item_j.close_ori
          taritemOf.close_adj = item_j.close_adj

          j += 1
        } else {
          break = true
        }
      }
            
      /** de adjust on combined quote data */
            
      prevNorm = taritemOf.close
      postNorm = taritemOf.close_ori
            
      taritemOf.high  = linearAdjust(taritemOf.high,  prevNorm, postNorm)
      taritemOf.low   = linearAdjust(taritemOf.low,   prevNorm, postNorm)
      taritemOf.open  = linearAdjust(taritemOf.open,  prevNorm, postNorm)
      taritemOf.close = linearAdjust(taritemOf.close, prevNorm, postNorm)
            
      loop(i + j)
    }; loop(masterFromIdx)
        
    val evt = new SerChangeEvent(targetQuoteSer,
                                 SerChangeEvent.Type.Updated,
                                 null,
                                 masterFromTime,
                                 targetQuoteSer.lastOccurredTime)
    targetQuoteSer.fireSerChangeEvent(evt)
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Float, prevNorm: Float, postNorm: Float): Float = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }
    
  def dispose: Unit = {
    if (sourceSerChangelistener != null) {
      sourceQuoteSer.removeSerChangeListener(sourceSerChangelistener)
    }
  }
}
