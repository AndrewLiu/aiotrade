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
package org.aiotrade.lib.math.timeseries.computable

import java.text.DecimalFormat
import org.aiotrade.lib.math.timeseries.Ser
import org.aiotrade.lib.util.CallBack
import scala.collection.mutable.ArrayBuffer


/**
 * A helper class to implement most of the Computable methods, it can be used
 * by indicator etc.
 *
 * @param baseSer:Ser base series to compute resultSer
 * @param resultSer:Indicatoe result series to be computed
 *
 * @author Caoyuan Deng
 */
class ComputableHelper(var baseSer:Ser, var self:Indicator) {
    
    /**
     * factors of this instance, such as period long, period short etc,
     * it's 'final' to avoid being replaced somewhere.
     */
    var _factors = new ArrayBuffer[Factor]
        
    private var baseSerChangeListener :SerChangeListener = _
    
    private var baseSerChangeEventCallBack :CallBack = _

    if (baseSer != null && self != null) {
        init(baseSer, self)
    }

    def this() {
        // * do nothing: factors should has been initialized in instance initialization procedure
        this(null, null)
    }
    
    def init(baseSer:Ser, self:Indicator) :Unit = {
        this.baseSer = baseSer
        this.self = self
        
        addBaseSerChangeListener
    }
    
    private def addBaseSerChangeListener :Unit = {
        /**
         * The series is a result computed from baseSeries, so
         * should follow the baseSeries' data changing:
         * 1. In case of series is the same as baseSeries, should repond
         *    to FinishingComputing event of baseSeries.
         * 2. In case of series is not the same as baseSeries, should repond
         *    to FinishedLoading, RefreshInLoading and Updated event of baseSeries.
         */
        if (self == baseSer) {
            
            baseSerChangeListener = new SerChangeListener {
                def serChanged(evt:SerChangeEvent) :Unit = {
                    import SerChangeEvent.Type._
                    val fromTime = evt.beginTime
                    evt.tpe match {
                        case FinishedLoading | RefreshInLoading | Updated =>
                            /**
                             * only responds to those events fired by outside for baseSer,
                             * such as loaded from a data server etc.
                             */
                            // * call back
                            self.computableActor ! ComputeFrom(fromTime)
                        case _ =>
                    }
                    
                    /** process event's callback, remember it to forwarded it in postCompute() late */
                    baseSerChangeEventCallBack = evt.callBack
                }
            }
            
        } else {
            
            baseSerChangeListener = new SerChangeListener {
                def serChanged(evt:SerChangeEvent) {
                    import SerChangeEvent.Type._
                    val begTime = evt.beginTime
                    evt.tpe match {
                        case FinishedLoading | RefreshInLoading | Updated | FinishedComputing =>
                            /**
                             * If the resultSer is the same as baseSer (such as QuoteSer),
                             * the baseSer will fire an event when compute() finished,
                             * then run to here, this may cause a dead loop. So, added
                             * FinishedComputing event to diff from Updated(caused by outside)
                             */
                            /** call back */
                            self.computableActor ! ComputeFrom(begTime)
                        case Clear =>
                            self clear begTime
                        case _ =>
                    }
                    
                    /** remember event's callback to be forwarded in postCompute() */
                    baseSerChangeEventCallBack = evt.callBack
                }
            }
            
        }
        
        baseSer.addSerChangeListener(baseSerChangeListener)
    }
    
    /**
     * preComputeFrom) will set and backup the context before computeFrom(long begTime):
     * begTime, begIdx etc.
     *
     *
     * @return begIdx
     */
    private var begTime:Long = _ // used by postComputeFrom only
    def preComputeFrom(begTime:Long) :Int = {
        assert(this.baseSer != null, "base series not set!")

        val timestamps = self.timestamps

        val (begTime1, begIdx, mayNeedToValidate) = if (begTime <= 0) {
            (begTime, 0, true)
        } else {
            if (begTime < self.computedTime) {
                // * the timestamps <-> items map may not be validate now, should validate it first
                val begTimeX = begTime
                // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
                val begIdxX = Math.max(timestamps.indexOfOccurredTime(begTimeX), 0) // should not less then 0
                (begTimeX, begIdxX, true)
            } else if (begTime > self.computedTime){
                // * if begTime > computedTime, re-compute from computedTime
                val begTimeX = self.computedTime
                // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
                val begIdxX = Math.max(timestamps.indexOfOccurredTime(begTimeX), 0) // should not less then 0
                (begTimeX, begIdxX, timestamps.size > self.items.size)
            } else {
                // * begTime == computedTime
                // * if begTime > computedTime, re-compute from computedTime
                val begTimeX = self.computedTime
                // * indexOfOccurredTime always returns physical index, so don't worry about isOncalendarTime
                val begIdxX = Math.max(timestamps.indexOfOccurredTime(begTimeX), 0) // should not less then 0
                (begTimeX, begIdxX, false)
            }
        }

        self.validate
        //        if (mayNeedToValidate) {
        //            self.validate
        //        }

        this.begTime = begTime1
                
        //println(resultSer.freq + resultSer.shortDescription + ": computed time=" + computedTime + ", begIdx=" + begIdx)
        /**
         * should re-compute series except it's also the baseSer:
         * @TODO
         * Do we really need clear it from begTime, or just from computed time after computing ?
         */
        //        if (resultSer != baseSer) {
        //            /** in case of resultSer == baseSer, do this will also clear baseSer */
        //            resultSer.clear(begTime);
        //        }
        
        begIdx
    }

    def postComputeFrom :Unit = {
        /** construct resultSer's change event, forward baseSerChangeEventCallBack */
        self.fireSerChangeEvent(new SerChangeEvent(self,
                                                   SerChangeEvent.Type.FinishedComputing,
                                                   null,
                                                   begTime,
                                                   self.computedTime,
                                                   baseSerChangeEventCallBack))
    }
    
    def addFactor(factor:Factor) :Unit = {
        /** add factor change listener to this factor */
        addFactorChangeListener(factor)
        
        _factors += factor
    }
    
    private def addFactorChangeListener(factor:Factor) :Unit = {
        factor.addFactorChangeListener(new FactorChangeListener() {
                def factorChanged(evt:FactorChangeEvent) :Unit = {
                    /**
                     * As any one of factor in factor changed will fire change events
                     * for each factor in factor, we only need respond to the first
                     * one.
                     * @see fireFacChangeEvents();
                     */
                    if (evt.getSource.equals(_factors(0))) {
                        // * call back
                        self.computableActor ! ComputeFrom(0)
                    }
                }
            })
    }

    def factors :ArrayBuffer[Factor] = _factors

    /**
     *
     *
     * @return if any value of factors changed, return true, else return false
     */
    def factors_=(factors:ArrayBuffer[Factor]) :Unit = {
        if (factors != null) {
            val values = new Array[Number](factors.size)
            for (i <- 0 until factors.size) {
                values(i) = _factors(i).value
            }
            factors_=(values)
        }
    }
    
    /**
     *
     *
     * @return if any value of factors changed, return true, else return false
     */
    def factors_=(facValues:Array[Number]) :Unit = {
        var valueChanged = false
        if (facValues != null) {
            if (factors.size == facValues.length) {
                for (i <- 0 until facValues.length) {
                    val myFactor = _factors(i)
                    val inValue = facValues(i)
                    /** check if changed happens before set myFactor */
                    if ((myFactor.value != inValue.floatValue)) {
                        valueChanged = true
                    }
                    myFactor.value = inValue
                }
            }
        }
        
        if (valueChanged) {
            fireFactorsChangeEvents
        }
    }
    
    private def fireFactorsChangeEvents :Unit = {
        factors.foreach{x => x.fireFactorChangeEvent(new FactorChangeEvent(x))}
    }
    
    def replaceFac(oldFactor:Factor, newFactor:Factor) :Unit = {
        var idxOld = -1
        var i = 0
        var break = false
        while (i < factors.size && !break) {
            val factor = factors(i);
            if (factor.equals(oldFactor)) {
                idxOld = i
                break = true
            }
        }
        
        if (idxOld != -1) {
            addFactorChangeListener(newFactor)
            
            factors(idxOld) = newFactor
        }
    }

    def dispose :Unit = {
        if (baseSerChangeListener != null) {
            baseSer.removeSerChangeListener(baseSerChangeListener)
        }
    }
    
}

object ComputableHelper {
    private val FAC_DECIMAL_FORMAT = new DecimalFormat("0.###")

    def displayName(ser:Ser) :String = ser match {
        case x:Computable => displayName(ser.shortDescription, x.factors)
        case _ => ser.shortDescription
    }

    def displayName(name:String, factors:ArrayBuffer[Factor]) :String = {
        val buffer = new StringBuffer(name)

        val size = factors.size
        for (i <- 0 until size) {
            if (i == 0) {
                buffer.append(" (")
            }
            buffer.append(FAC_DECIMAL_FORMAT.format(factors(i).value))
            if (i < size - 1) {
                buffer.append(", ")
            } else {
                buffer.append(")")
            }
        }

        buffer.toString
    }

}
