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
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TUnit
import scala.swing.Reactions
import scala.swing.Reactor

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSerCombiner(sourceSer: QuoteSer, targetSer: QuoteSer, timeZone: TimeZone) extends Reactor {

  private val cal = Calendar.getInstance(timeZone)

  private val sourceSerReaction: Reactions.Reaction = {
    case TSerEvent.FinishedComputing(_, _, fromTime, _, _, _) =>
      computeCont(fromTime)
  }
  
  reactions += sourceSerReaction
  listenTo(sourceSer)
    
  def computeFrom(fromTime: Long): Unit = {
    computeCont(fromTime)
  }
    
  /**
   * Combine data according to wanted frequency, such as Weekly, Monthly etc.
   */
  protected def computeCont(fromTime: Long): Unit = {
    val targetUnit = targetSer.freq.unit
        
    val masterFromTime = targetUnit.beginTimeOfUnitThatInclude(fromTime, cal)
    val masterFromIdx1 = sourceSer.timestamps.indexOfNearestOccurredTimeBehind(masterFromTime)
    val masterFromIdx = if (masterFromIdx1 < 0) 0 else masterFromIdx1
        
    //System.out.println("myFromIdx: " + myFromIdx + " materFromIdx: " + masterFromIdx);
    //targetQuoteSer.clear(myFromTime);
        
    /** begin combining: */
                
        
    val l = sourceSer.size
    //for (i <- masterFromIdx until size) {
    def loop(i: Int): Unit = {
      if (i >= l) return
            
      val time_i = sourceSer.timeOfIndex(i)
      if (time_i < masterFromTime) {
        loop(i + 1)
      }
            
      val intervalBegin = targetUnit.beginTimeOfUnitThatInclude(time_i, cal)
            
      cal.setTimeInMillis(intervalBegin)
      val currWeekOfYear  = cal.get(Calendar.WEEK_OF_YEAR)
      val currMonthOfYear = cal.get(Calendar.MONTH)
      val currYear        = cal.get(Calendar.YEAR)
            
      targetSer.createOrClear(intervalBegin)
            
      var prevNorm = sourceSer.close(time_i)
      var postNorm = sourceSer.close_adj(time_i)
            
      targetSer.open(intervalBegin)   = linearAdjust(sourceSer.open(time_i),  prevNorm, postNorm)
      targetSer.high(intervalBegin)   = -Float.MaxValue
      targetSer.low(intervalBegin)    = +Float.MaxValue
      targetSer.volume(intervalBegin) = 0
            
      /** compose followed source data of this interval to targetData */
      var j = 0
      var break = false
      while (i + j < l && !break) {
        val time_j = sourceSer.indexOfTime(i + j)
                
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
          prevNorm = sourceSer.close(time_j)
          postNorm = sourceSer.close_adj(time_j)

          targetSer.high(intervalBegin)  = Math.max(targetSer.high(intervalBegin), linearAdjust(sourceSer.high(time_j),  prevNorm, postNorm))
          targetSer.low(intervalBegin)   = Math.min(targetSer.low(intervalBegin),  linearAdjust(sourceSer.low(time_j),   prevNorm, postNorm))
          targetSer.close(intervalBegin) = linearAdjust(sourceSer.close(time_j),   prevNorm, postNorm)

          targetSer.volume(intervalBegin) = targetSer.volume(intervalBegin) + sourceSer.volume(time_j)

          targetSer.close_ori(intervalBegin) = sourceSer.close_ori(time_j)
          targetSer.close_adj(intervalBegin) = sourceSer.close_adj(time_j)

          j += 1
        } else {
          break = true
        }
      }
            
      /** de adjust on combined quote data */
            
      prevNorm = targetSer.close(intervalBegin)
      postNorm = targetSer.close_ori(intervalBegin)
            
      targetSer.high(intervalBegin)  = linearAdjust(targetSer.high(intervalBegin),  prevNorm, postNorm)
      targetSer.low(intervalBegin)   = linearAdjust(targetSer.low(intervalBegin),   prevNorm, postNorm)
      targetSer.open(intervalBegin)  = linearAdjust(targetSer.open(intervalBegin),  prevNorm, postNorm)
      targetSer.close(intervalBegin) = linearAdjust(targetSer.close(intervalBegin), prevNorm, postNorm)
            
      loop(i + j)
    }; loop(masterFromIdx)
        
    val evt = TSerEvent.Updated(targetSer,
                                null,
                                masterFromTime,
                                targetSer.lastOccurredTime)
    targetSer.publish(evt)
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Float, prevNorm: Float, postNorm: Float): Float = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }
    
  def dispose: Unit = {
    deafTo(sourceSer)
    if (sourceSerReaction != null) {
      reactions -= sourceSerReaction
    }
  }
}
