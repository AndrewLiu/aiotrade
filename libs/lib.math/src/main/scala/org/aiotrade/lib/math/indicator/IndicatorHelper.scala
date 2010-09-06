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
package org.aiotrade.lib.math.indicator

import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.util.actors.Reactions
import org.aiotrade.lib.util.actors.Reactor


/**
 * A helper class to implement most of the Indicator methods, it can be used
 * by indicator etc.
 *
 * @param baseSer:Ser base series to compute resultSer
 * @param resultSer:Indicatoe result series to be computed
 *
 * @author Caoyuan Deng
 */
trait IndicatorHelper extends Reactor {self: Indicator =>

  /**
   * factors of this instance, such as period long, period short etc,
   * @todo it should be 'final' to avoid being replaced somewhere?.
   */
  var _factors = Array[Factor]()
        
  /**
   * preComputeFrom will set and backup the context before computeFrom(long begTime):
   * begTime, begIdx etc.
   *
   *
   * @return begTime
   */
  private var fromTime: Long = _ // used by postComputeFrom only

  private var baseSerReactions: Reactions.Reaction = _
  // remember event's callback to be forwarded in postCompute()
  private var baseSerEventCallBack: TSerEvent.Callback = _

  protected def setBaseSer(baseSer: BaseTSer) {
    self.baseSer = baseSer

    // * share same timestamps with baseSer, should be care of ReadWriteLock
    self.attach(baseSer.timestamps)

    addBaseSerReactions
  }

  private def addBaseSerReactions {
    /**
     * The ser is a result computed from baseSer, so should follow the baseSeries' data changing:
     * 1. In case of series is the same as baseSeries, should respond to
     *    Computed event of baseSeries.
     * 2. In case of series is not the same as baseSeries, should respond to
     *    Loaded, Refresh and Updated event of baseSeries.
     */
    baseSerReactions = {
      case TSerEvent.Loaded(_, _, fromTime, toTime, _, callback) =>
        self.computeFrom(fromTime)
        baseSerEventCallBack = callback
      case TSerEvent.Refresh(_, _, fromTime, toTime, _, callback) =>
        self.computeFrom(fromTime)
        baseSerEventCallBack = callback
      case TSerEvent.Updated(_, _, fromTime, toTime, _, callback) =>
        self.computeFrom(fromTime)
        baseSerEventCallBack = callback
      case TSerEvent.Computed(src, _, fromTime, toTime, _, callback) if (src eq baseSer) && (src ne this) =>
        /**
         * If the resultSer is the same as baseSer (such as QuoteSer),
         * the baseSer will fire an event when compute() finished,
         * then run to here, this may cause a dead loop. So, FinishedComputing
         * should not react when self eq baseSer
         */
        self.computeFrom(fromTime)
        baseSerEventCallBack = callback
      case TSerEvent.Cleared(src, _, fromTime, toTime, _, callback) if (src eq baseSer) && (src ne this) =>
        self.clear(fromTime)
        baseSerEventCallBack = callback
    }
    
    self.reactions += baseSerReactions
    self.listenTo(baseSer)
  }
        
  def preComputeFrom(fromTime: Long): Int = {
    assert(this.baseSer != null, "base series not set!")

    val timestamps = self.timestamps

    val (fromTime1, fromIdx, mayNeedToValidate) = if (fromTime <= 0) {
      (fromTime, 0, true)
    } else {
      if (fromTime < self.computedTime) {
        // * the timestamps <-> items map may not be validate now, should validate it first
        val fromTimeX = fromTime
        // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
        val fromIdxX = math.max(timestamps.indexOfOccurredTime(fromTimeX), 0) // should not less then 0
        (fromTimeX, fromIdxX, true)
      } else if (fromTime > self.computedTime){
        // * if begTime > computedTime, re-compute from computedTime
        val fromTimeX = self.computedTime
        // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
        val fromIdxX = math.max(timestamps.indexOfOccurredTime(fromTimeX), 0) // should not less then 0
        (fromTimeX, fromIdxX, timestamps.size > self.size)
      } else {
        // * begTime == computedTime
        // * if begTime > computedTime, re-compute from computedTime
        val fromTimeX = self.computedTime
        // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
        val fromIdxX = math.max(timestamps.indexOfOccurredTime(fromTimeX), 0) // should not less then 0
        (fromTimeX, fromIdxX, false)
      }
    }

    if (this ne baseSer) {
      self.validate
    }

    //        if (mayNeedToValidate) {
    //            self.validate
    //        }

    this.fromTime = fromTime1
                
    //println(resultSer.freq + resultSer.shortDescription + ": computed time=" + computedTime + ", begIdx=" + begIdx)
    /**
     * should re-compute series except it's also the baseSer:
     * @TODO
     * Do we really need clear it from begTime, or just from computed time after computing ?
     */
    //        if (resultSer != baseSer) {
    //            /** in case of resultSer == baseSer, do this will also clear baseSer */
    //            resultSer.clear(fromTime);
    //        }
        
    fromIdx
  }

  def postComputeFrom {
    // construct resultSer's change event, forward baseTSerEventCallBack
    self.publish(TSerEvent.Computed(self,
                                    null,
                                    fromTime,
                                    self.computedTime,
                                    null,
                                    baseSerEventCallBack))
  }
    
  def addFactor(factor: Factor) {
    /** add factor reaction to this factor */
    addFactorReactions(factor)

    val old = _factors
    _factors = new Array[Factor](old.length + 1)
    System.arraycopy(old, 0, _factors, 0, old.length)
    _factors(_factors.length - 1) = factor
  }
    
  private def addFactorReactions(factor: Factor) {
    reactions += {
      case FactorEvent(source) =>
        /**
         * As any factor in factors changed will publish events
         * for each factor in factors, we only need respond to the first
         * one.
         */
        if (source.equals(_factors(0))) {
          computeFrom(0)
        }

    }
    listenTo(factor)

  }

  def factors: Array[Factor] = _factors

  /**
   * @return if any value of factors changed, return true, else return false
   */
  def factors_=(factors: Array[Factor]) {
    if (factors != null) {
      val values = new Array[Double](factors.length)
      for (i <- 0 until factors.length) {
        values(i) = _factors(i).value
      }
      factorValues_=(values)
    }
  }
    
  /**
   *
   * @return if any value of factors changed, return true, else return false
   */
  def factorValues_=(facValues: Array[Double]) {
    var valueChanged = false
    if (facValues != null) {
      if (factors.length == facValues.length) {
        var i = 0
        while (i < facValues.length) {
          val myFactor = _factors(i)
          val inValue = facValues(i)
          /** check if changed happens before set myFactor */
          if (myFactor.value != inValue) {
            valueChanged = true
          }
          myFactor.value = inValue
          i += 1
        }
      }
    }
        
    if (valueChanged) {
      factors foreach {x => publish(FactorEvent(x))}
    }
  }
    
  def replaceFactor(oldFactor: Factor, newFactor: Factor): Unit = {
    var idxOld = -1
    var i = 0
    var break = false
    while (i < factors.length && !break) {
      val factor = factors(i)
      if (factor.equals(oldFactor)) {
        idxOld = i
        break = true
      }
    }
        
    if (idxOld != -1) {
      addFactorReactions(newFactor)
            
      factors(idxOld) = newFactor
    }
  }

  def dispose {
    if (baseSerReactions != null) {
      baseSer.reactions -= baseSerReactions
    }
  }
    
}
