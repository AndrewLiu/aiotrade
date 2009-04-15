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
import org.aiotrade.lib.math.timeseries.{Ser,SerChangeEvent,SerChangeListener}
import org.aiotrade.lib.util.CallBack
import scala.collection.mutable.ArrayBuffer


/**
 * A helper class to implement most of the Computable methods, it can be used
 * by indicator etc.
 *
 * @param baseSer:Ser base series to compute resultSer
 * @param resultSer:Indicatoe result series to be computed
 * @author Caoyuan Deng
 */
class ComputableHelper(var baseSer:Ser, var resultSer:Indicator) {
    
    /**
     * opts of this instance, such as period long, period short etc,
     * it's 'final' to avoid being replaced somewhere.
     */
    var _opts = new ArrayBuffer[Opt]
        
    private var baseSerChangeListener :SerChangeListener = _
    
    private var baseSerChangeEventCallBack :CallBack = _

    if (baseSer != null && resultSer != null) {
        init(baseSer, resultSer)
    }

    def this() {
        /** do nothing: opts should has been initialized in instance initialization procedure */
        this(null, null)
    }
    
    def init(baseSer:Ser, resultSer:Indicator) :Unit = {
        this.baseSer = baseSer
        this.resultSer = resultSer
        
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
        if (resultSer == baseSer) {
            
            baseSerChangeListener = new SerChangeListener() {
                def serChanged(evt:SerChangeEvent) :Unit = {
                    import SerChangeEvent.Type._
                    val fromTime = evt.beginTime
                    evt.tpe match {
                        case FinishedLoading | RefreshInLoading | Updated =>
                            /**
                             * only responds to those events fired by outside for baseSer,
                             * such as loaded from a data server etc.
                             */
                            /** call back */
                            resultSer ! (Compute, fromTime)
                            
                        case _ =>
                    }
                    
                    /** process event's callback, remember it to forwarded it in postCompute() late */
                    baseSerChangeEventCallBack = evt.callBack
                }
            };
            
        } else {
            
            baseSerChangeListener = new SerChangeListener() {
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
                            resultSer ! (Compute, begTime)
                        case Clear =>
                            resultSer clear begTime
                        case _ =>
                    }
                    
                    /** remember event's callback to be forwarded in postCompute() */
                    baseSerChangeEventCallBack = evt.callBack
                }
            };
            
        }
        
        baseSer.addSerChangeListener(baseSerChangeListener)
    }
    
    /**
     * preComputeFrom() will set and backup the context before computeFrom(long begTime):
     * begTime, begIdx etc.
     *
     *
     * @return begIdx
     */
    private var begTime:Long = _
    def preComputeFrom(begTime:Long) :Int = {
        assert(this.baseSer != null, "base series not set!")
        
        this.begTime = begTime;
        val computedTime = resultSer.computedTime
        
        val begIdx = if (begTime < computedTime || begTime == 0) {
            0
        } else {
            /** if computedTime < begTime, re-compute from the minimal one */
            this.begTime = Math.min(computedTime, begTime);
            
            /** indexOfTime always return physical index, so don't worry about isOncalendarTime() */
            val begIdx1 = this.baseSer.indexOfOccurredTime(begTime);
            if (begIdx1 < 0) 0 else begIdx1
        }
        
        /**
         * should re-compute series except it's also the baseSer:
         * @TODO
         * Do we really need clear it from begTime, or just from computed time after computing?
         */
        //        if (resultSer != baseSer) {
        //            /** in case of resultSer == baseSer, do this will also clear baseSer */
        //            resultSer.clear(begTime);
        //        }
        
        begIdx
    }
    
    def postComputeFrom :Unit = {
        /** construct resultSer's change event, forward baseSerChangeEventCallBack */
        resultSer.fireSerChangeEvent(new SerChangeEvent(resultSer,
                                                        SerChangeEvent.Type.FinishedComputing,
                                                        null,
                                                        begTime,
                                                        resultSer.computedTime,
                                                        baseSerChangeEventCallBack))
    }
    
    def addOpt(opt:Opt) :Unit = {
        /** add opt change listener to this opt */
        addOptChangeListener(opt)
        
        _opts += opt
    }
    
    private def addOptChangeListener(opt:Opt) :Unit = {
        opt.addOptChangeListener(new OptChangeListener() {
                def optChanged(evt:OptChangeEvent) :Unit = {
                    /**
                     * As any one of opt in opt changed will fire change events
                     * for each opt in opt, we only need respond to the first
                     * one.
                     * @see fireOptChangeEvents();
                     */
                    if (evt.getSource.equals(_opts(0))) {
                        /** call back */
                        resultSer.computeFrom(0)
                    }
                }
            })
    }

    def opts :ArrayBuffer[Opt] = _opts

    /**
     *
     *
     * @return if any value of opts changed, return true, else return false
     */
    def opts_=(opts:ArrayBuffer[Opt]) :Unit = {
        if (opts != null) {
            val values = new Array[Number](opts.size)
            for (i <- 0 until opts.size) {
                values(i) = _opts(i).value
            }
            opts_=(values)
        }
    }
    
    /**
     *
     *
     * @return if any value of opts changed, return true, else return false
     */
    def opts_=(values:Array[Number]) :Unit = {
        var valueChanged = false;
        if (values != null) {
            if (opts.size == values.length) {
                for (i <- 0 until values.length) {
                    val myOpt = _opts(i)
                    val inValue = values(i)
                    /** check if changed happens before set myOpt */
                    if ((myOpt.value != inValue.floatValue)) {
                        valueChanged = true
                    }
                    myOpt.value = inValue
                }
            }
        }
        
        if (valueChanged) {
            fireOptsChangeEvents
        }
    }
    
    private def fireOptsChangeEvents :Unit = {
        for (opt <- opts) {
            opt.fireOptChangeEvent(new OptChangeEvent(opt))
        }
    }
    
    def replaceOpt(oldOpt:Opt, newOpt:Opt) :Unit = {
        var idxOld = -1
        var i = 0
        var break = false
        while (i < opts.size && !break) {
            val opt = opts(i);
            if (opt.equals(oldOpt)) {
                idxOld = i
                break = true
            }
        }
        
        if (idxOld != -1) {
            addOptChangeListener(newOpt)
            
            opts(idxOld) = newOpt
        }
    }

    def dispose :Unit = {
        if (baseSerChangeListener != null) {
            baseSer.removeSerChangeListener(baseSerChangeListener)
        }
    }
    
}

object ComputableHelper {
    private val OPT_DECIMAL_FORMAT = new DecimalFormat("0.###")

    def displayName(ser:Ser) :String = ser match {
        case x:Computable => displayName(ser.shortDescription, x.opts)
        case _ => ser.shortDescription
    }

    def displayName(name:String, opts:ArrayBuffer[Opt]) :String = {
        val buffer = new StringBuffer(name)

        val size = opts.size
        for (i <- 0 until size) {
            if (i == 0) {
                buffer.append(" (")
            }
            buffer.append(OPT_DECIMAL_FORMAT.format(opts(i).value))
            if (i < size - 1) {
                buffer.append(", ")
            } else {
                buffer.append(")")
            }
        }

        buffer.toString
    }

}
