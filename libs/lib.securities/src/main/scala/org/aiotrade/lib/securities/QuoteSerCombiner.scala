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
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.util.actors.Reactor
import scala.annotation.tailrec

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSerCombiner(srcSer: QuoteSer, tarSer: QuoteSer, timeZone: TimeZone) extends Reactor {
  private val log = Logger.getLogger(this.getClass.getName)

  reactions += {
    case TSerEvent.Loaded(_, _, fromTime, _, _, _) => computeCont(fromTime)
    case TSerEvent.Computed(_, _, fromTime, _, _, _) => computeCont(fromTime)
    case TSerEvent.Updated(_, _, fromTime, _, _, _) => computeCont(fromTime)
    case TSerEvent.Cleared(_, _, fromTime, _, _, _) => computeCont(fromTime)
  }
  
  listenTo(srcSer)
    
  def computeFrom(fromTime: Long) {
    computeCont(fromTime)
  }
    
  /**
   * Combine data according to wanted frequency, such as Weekly, Monthly etc.
   */
  protected def computeCont(fromTime: Long) {
    val tarFreq = tarSer.freq
    val tarUnit = tarFreq.unit

    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(fromTime)
    val masterFromTime = tarUnit.round(cal)
    val masterFromIdx1 = srcSer.timestamps.indexOfNearestOccurredTimeBehind(masterFromTime)
    val masterFromIdx = if (masterFromIdx1 < 0) 0 else masterFromIdx1

    log.info(" masterFromIdx: " + masterFromIdx)
    //targetQuoteSer.clear(myFromTime);
        
    // --- begin combining:
                
    val l = srcSer.size
    
    @tailrec
    def loop(i: Int): Unit = {
      if (i >= l) return
            
      val time_i = srcSer.timeOfIndex(i)
      if (time_i < masterFromTime) {
        loop(i + 1)
      }
            
      val intervalBegin = tarUnit.beginTimeOfUnitThatInclude(time_i, cal)
            
      tarSer.createOrClear(intervalBegin)
            
      var prevNorm = srcSer.close(time_i)
      var postNorm = srcSer.close_adj(time_i)
            
      tarSer.open(intervalBegin)   = linearAdjust(srcSer.open(time_i),  prevNorm, postNorm)
      tarSer.high(intervalBegin)   = Double.MinValue
      tarSer.low(intervalBegin)    = Double.MaxValue
      tarSer.volume(intervalBegin) = 0
      tarSer.amount(intervalBegin) = 0
            
      /** compose followed source data of this interval to targetData */
      var j = 0
      var break = false
      while (i + j < l && !break) {
        val time_j = srcSer.timeOfIndex(i + j)
                
        /**
         * @NOTICE
         * There is strange behave of JDK's Calendar when a very big Long
         * timeInMillis assigned to it. so anyway, we let inSamePeriod = true
         * if j == 0, because jdata is the same data as idata in this case:
         */
        val inSameInterval = if (j == 0) true else {
          tarFreq.sameInterval(time_i, time_j, cal)
        }
        
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
          prevNorm = srcSer.close(time_j)
          postNorm = srcSer.close_adj(time_j)

          tarSer.high(intervalBegin)  = math.max(tarSer.high(intervalBegin), linearAdjust(srcSer.high(time_j),  prevNorm, postNorm))
          tarSer.low(intervalBegin)   = math.min(tarSer.low(intervalBegin),  linearAdjust(srcSer.low(time_j),   prevNorm, postNorm))
          tarSer.close(intervalBegin) = linearAdjust(srcSer.close(time_j),   prevNorm, postNorm)

          tarSer.volume(intervalBegin) = tarSer.volume(intervalBegin) + srcSer.volume(time_j)
          tarSer.amount(intervalBegin) = tarSer.amount(intervalBegin) + srcSer.amount(time_j)

          tarSer.close_ori(intervalBegin) = srcSer.close_ori(time_j)
          tarSer.close_adj(intervalBegin) = srcSer.close_adj(time_j)

          j += 1
        } else {
          break = true
        }
      }
            
      /** de adjust on combined quote data */
            
      prevNorm = tarSer.close(intervalBegin)
      postNorm = tarSer.close_ori(intervalBegin)
            
      tarSer.high(intervalBegin)  = linearAdjust(tarSer.high(intervalBegin),  prevNorm, postNorm)
      tarSer.low(intervalBegin)   = linearAdjust(tarSer.low(intervalBegin),   prevNorm, postNorm)
      tarSer.open(intervalBegin)  = linearAdjust(tarSer.open(intervalBegin),  prevNorm, postNorm)
      tarSer.close(intervalBegin) = linearAdjust(tarSer.close(intervalBegin), prevNorm, postNorm)
            
      loop(i + j)
    }

    loop(masterFromIdx)
        
    val evt = TSerEvent.Updated(tarSer, null, masterFromTime, tarSer.lastOccurredTime)
    log.info("Publishing " + evt)
    tarSer.publish(evt)
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }
    
  def dispose {}
}
