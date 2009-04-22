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
package org.aiotrade.lib.indicator;

import org.aiotrade.lib.indicator.function.Direction
import org.aiotrade.lib.indicator.function.AbstractFunction;
import org.aiotrade.lib.indicator.function.Function;
import org.aiotrade.lib.indicator.function.ZIGZAGFunction;
import org.aiotrade.lib.indicator.function.ADXFunction;
import org.aiotrade.lib.indicator.function.ADXRFunction;
import org.aiotrade.lib.indicator.function.BOLLFunction;
import org.aiotrade.lib.indicator.function.CCIFunction;
import org.aiotrade.lib.indicator.function.DIFunction;
import org.aiotrade.lib.indicator.function.DMFunction;
import org.aiotrade.lib.indicator.function.DXFunction;
import org.aiotrade.lib.indicator.function.EMAFunction;
import org.aiotrade.lib.indicator.function.MACDFunction;
import org.aiotrade.lib.indicator.function.MAFunction;
import org.aiotrade.lib.indicator.function.MAXFunction;
import org.aiotrade.lib.indicator.function.MFIFunction;
import org.aiotrade.lib.indicator.function.MINFunction;
import org.aiotrade.lib.indicator.function.MTMFunction;
import org.aiotrade.lib.indicator.function.OBVFunction;
import org.aiotrade.lib.indicator.function.PROBMASSFunction;
import org.aiotrade.lib.indicator.function.ROCFunction;
import org.aiotrade.lib.indicator.function.RSIFunction;
import org.aiotrade.lib.indicator.function.SARFunction;
import org.aiotrade.lib.indicator.function.STDDEVFunction;
import org.aiotrade.lib.indicator.function.STOCHDFunction;
import org.aiotrade.lib.indicator.function.STOCHJFunction;
import org.aiotrade.lib.indicator.function.STOCHKFunction;
import org.aiotrade.lib.indicator.function.SUMFunction;
import org.aiotrade.lib.indicator.function.TRFunction;
import org.aiotrade.lib.indicator.function.WMSFunction;
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.computable.DefaultFactor;
import org.aiotrade.lib.math.timeseries.QuoteSer;
import org.aiotrade.lib.math.timeseries.DefaultSer;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.math.timeseries.computable.ComputableHelper
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
object AbstractIndicator {
    val NaN = Float.NaN

    /** a static global session id */
    protected var sessionId:Long = _

    protected def setSessionId :Unit = {
        sessionId += 1
    }

    /**
     * a helper function for keeping the same functin form as Function, don't be
     * puzzled by the name, it actully will return funcion instance
     */
    protected def getInstance[T <: Function](clazz:Class[T], baseSer:Ser, args:Any*) :T = {
        AbstractFunction.getInstance(clazz, baseSer, args:_*)
    }

    // ----- Functions for test
    protected def crossOver(idx:Int, var1:Var[Float], var2:Var[Float]) :Boolean = {
        if (idx > 0) {
            if (var1(idx) >= var2(idx) &&
                var1(idx - 1) < var2(idx - 1)) {
                return true
            }
        }
        false
    }

    protected def crossOver(idx:int, var1:Var[Float], value:Float) :Boolean = {
        if (idx > 0) {
            if (var1(idx) >= value &&
                var1(idx - 1) < value) {
                return true
            }
        }
        false
    }

    protected def crossUnder(idx:Int, var1:Var[Float], var2:Var[Float]) :Boolean = {
        if (idx > 0) {
            if (var1(idx) < var2(idx) &&
                var1(idx - 1) >= var2(idx - 1)) {
                return true
            }
        }
        false
    }

    protected def crossUnder(idx:int, var1:Var[Float], value:Float) :Boolean = {
        if (idx > 0) {
            if (var1(idx) < value &&
                var1(idx - 1) >= value) {
                true
            }
        }
        false
    }

    protected def turnUp(idx:Int, var1:Var[Float]) :Boolean = {
        if (idx > 1) {
            if (var1(idx) > var1(idx - 1) &&
                var1(idx - 1) <= var1(idx - 2)) {
                return true
            }
        }
        false
    }

    protected def turnDown(idx:Int, var1:Var[Float]) :Boolean = {
        if (idx > 1) {
            if (var1(idx) < var1(idx - 1) &&
                var1(idx - 1) >= var1(idx - 2)) {
                return true
            }
        }
        false
    }

    // ----- End of functions for test

}

abstract class AbstractIndicator(baseSer:Ser) extends DefaultSer with Indicator {
    import AbstractIndicator._
    
    /**
     * !NOTICE
     * computableHelper should be created here, because it will be used to
     * inject Factor(s): new Factor() will call addFac which delegated
     * by computableHelper.addFac(..)
     */
    private val computableHelper = new ComputableHelper
    private var _computedTime :Long = -Long.MaxValue
    
    /** some instance scope variables that can be set directly */
    protected var _overlapping = false
    protected var _sname = "unkown"
    protected var _lname = "unkown"
    
    /**
     * horizonal _grids of this indicator used to draw grid
     */
    protected var _grids :Array[Float] = _
    
    /** base series to compute this */
    protected var _baseSer:Ser = _
    
    /** To store values of open, high, low, close, volume: */
    protected var O :Var[Float] = _
    protected var H: Var[Float] = _
    protected var L: Var[Float] = _
    protected var C: Var[Float] = _
    protected var V: Var[Float] = _
    
    init(baseSer)
    
    /**
     * Make sure this null args contructor only be called and return instance to
     * NetBeans layer manager for register usage, so it just do nothing.
     */
    def this() {
        /** do nothing: computableHelper should has been initialized in instance scope */
        this(null)
    }
    
    
    /**
     * make sure this method will be called before this instance return to any others:
     * 1. via constructor (except the no-arg constructor)
     * 2. via createInstance
     */
    def init(baseSer:Ser) :Unit = {
        if (baseSer != null) {
            super.init(baseSer.freq)
            this._baseSer = baseSer

            // * share same timestamps with baseSer, should be care of ReadWriteLock
            this.attach(baseSer.timestamps)
            
            this.computableHelper.init(baseSer, this)
        
            initPredefinedVarsOfBaseSer

            // * actor should explicitly start
            start
        }
    }
    
    /** override this method to define your predefined vars */
    protected def initPredefinedVarsOfBaseSer :Unit = {
        _baseSer match {
            case x:QuoteSer =>
                O = x.open
                H = x.high
                L = x.low
                C = x.close
                V = x.volume
            case _ =>
        }
    }
    
    protected def addFactor(factor:Factor) : Unit = {
        computableHelper.addFactor(factor)
    }
    
    def factors :ArrayBuffer[Factor] = {
        computableHelper.factors
    }
    
    def factors_=(factors:ArrayBuffer[Factor]) :Unit = {
        computableHelper.factors = factors
    }
    
    def factors_=(facValues:Array[Number]) :Unit = {
        computableHelper.factors = facValues
    }
    
    def grids :Array[Float] = _grids
    
    def isOverlapping :Boolean = _overlapping
    def overlapping_=(b:Boolean) = {
        _overlapping = b
    }
    
    def computedTime :Long = _computedTime
    
    /**
     * @NOTE
     * It's better to fire ser change events or fac change event instead of
     * call me directly. But, in case of baseSer has been loaded, there may
     * be no more ser change events fired, so when first create, call computeFrom(0)
     * is a safe maner.
     *
     * @TODO
     * Should this method synchronized?
     * As each seriesProvider has its own indicator instance, and indicator instance
     * usually called by chartview, that means, they are called usually in same
     * thread: awt.event.thread.
     *
     *
     * @param begin time to be computed
     */
    def computeFrom(begTime:Long) :Unit = {
        setSessionId

        /**
         * get baseSer's itemList size via protected _itemSize here instead of by
         * indicator's subclass when begin computeCont, because we could not
         * sure if the baseSer's _itemSize size has been change by others
         * (DataServer etc.)
         *
         * @Note
         * It's better to pass itemSize as param to computeCont instead of keep it as instance field,
         * so, we do not need to worry about if field _itemSize will be changed concurrent by another
         * thread
         */
        try {
            timestamps.readLock.lock

            val size = timestamps.size
            val begIdx = computableHelper.preComputeFrom(begTime)
            
            assert(timestamps.size == items.size,
                   "Should validate " + shortDescription + "first! : " +
                   "timestamps size=" + timestamps.size +
                   ", items size=" + items.size)

            computeCont(begIdx, size)
        
            computableHelper.postComputeFrom
        } finally {
            timestamps.readLock.unlock
        }
        
        _computedTime = timestamps.lastOccurredTime
    }
    
    protected def preComputeFrom(begTime:Long) :Int = {
        computableHelper.preComputeFrom(begTime)
    }
    
    def postComputeFrom :Unit = {
        computableHelper.postComputeFrom
    }
    
    protected def computeCont(begIdx:Int, size:Int) :Unit
    
    protected def longDescription :String = {
        _lname
    }
    
    override
    def shortDescription :String = {
        _sname
    }
    
    override
    def shortDescription_=(description:String) :Unit = {
        this._sname = description
    }
    
    override
    def toString :String = {
        if (longDescription != null) {
            shortDescription + " - " + longDescription
        } else shortDescription
    }
    
    def compare(another:Indicator) :Int = {
        if (this.toString.equalsIgnoreCase(another.toString)) {
            if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
        } else {
            this.toString.compareTo(another.toString)
        }
    }
    
    def createNewInstance(baseSer:Ser) :Indicator = {
        try {
            val instance = this.getClass.newInstance.asInstanceOf[Indicator]
            instance.init(baseSer)
            
            instance
        } catch {
            case ex:IllegalAccessException => ex.printStackTrace; null
            case ex:InstantiationException => ex.printStackTrace; null
        }
    }
    
    /**
     * Define functions
     * --------------------------------------------------------------------
     */
    
    /**
     * Functions
     * ----------------------------------------------------------------------
     */
    
    protected def sum(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[SUMFunction], _baseSer, baseVar, period).sum(sessionId, idx)
    }
    
    protected def max(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[MAXFunction], _baseSer, baseVar, period).max(sessionId, idx)
    }
    
    protected def min(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[MINFunction], _baseSer, baseVar, period).min(sessionId, idx)
    }
    
    protected def ma(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[MAFunction], _baseSer, baseVar, period).ma(sessionId, idx)
    }
    
    protected def ema(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[EMAFunction], _baseSer, baseVar, period).ema(sessionId, idx)
    }
    
    protected def stdDev(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[STDDEVFunction], _baseSer, baseVar, period).stdDev(sessionId, idx)
    }
    
    protected def probMass(idx:Int, baseVar:Var[Float], period:Factor, nInterval:Factor) :Array[Array[Float]] = {
        getInstance(classOf[PROBMASSFunction], _baseSer, baseVar, null, period, nInterval).probMass(sessionId, idx)
    }
    
    protected def probMass(idx:Int, baseVar:Var[Float], weight:Var[Float], period:Factor, nInterval:Factor) :Array[Array[Float]] = {
        getInstance(classOf[PROBMASSFunction], _baseSer, baseVar, weight, period, nInterval).probMass(sessionId, idx)
    }
    
    protected def tr(idx:Int) :Float = {
        getInstance(classOf[TRFunction], _baseSer).tr(sessionId, idx)
    }
    
    protected def dmPlus(idx:Int) :Float = {
        getInstance(classOf[DMFunction], _baseSer).dmPlus(sessionId, idx)
    }
    
    protected def dmMinus(idx:Int) :Float = {
        getInstance(classOf[DMFunction], _baseSer).dmMinus(sessionId, idx)
    }
    
    protected def diPlus(idx:Int, period:Factor) :Float = {
        getInstance(classOf[DIFunction], _baseSer, period).diPlus(sessionId, idx)
    }
    
    protected def diMinus(idx:Int, period:Factor) :Float = {
        getInstance(classOf[DIFunction], _baseSer, period).diMinus(sessionId, idx)
    }
    
    protected def dx(idx:Int, period:Factor) :Float = {
        getInstance(classOf[DXFunction], _baseSer, period).dx(sessionId, idx)
    }
    
    protected def adx(idx:Int, periodDi:Factor, periodAdx:Factor) :Float = {
        getInstance(classOf[ADXFunction], _baseSer, periodDi, periodAdx).adx(sessionId, idx)
    }
    
    protected def adxr(idx:Int, periodDi:Factor, periodAdx:Factor) :Float = {
        getInstance(classOf[ADXRFunction], _baseSer, periodDi, periodAdx).adxr(sessionId, idx)
    }
    
    protected def bollMiddle(idx:Int, baseVar:Var[_], period:Factor, alpha:Factor) :Float = {
        getInstance(classOf[BOLLFunction], _baseSer, baseVar, period, alpha).bollMiddle(sessionId, idx)
    }
    
    protected def bollUpper(idx:Int, baseVar:Var[_], period:Factor, alpha:Factor) :Float = {
        getInstance(classOf[BOLLFunction], _baseSer, baseVar, period, alpha).bollUpper(sessionId, idx)
    }
    
    protected def bollLower(idx:Int, baseVar:Var[_], period:Factor, alpha:Factor) :Float = {
        getInstance(classOf[BOLLFunction], _baseSer, baseVar, period, alpha).bollLower(sessionId, idx)
    }
    
    protected def cci(idx:Int, period:Factor, alpha:Factor) :Float = {
        getInstance(classOf[CCIFunction], _baseSer, period, alpha).cci(sessionId, idx)
    }
    
    protected def macd(idx:Int, baseVar:Var[_], periodSlow:Factor, periodFast:Factor) :Float = {
        getInstance(classOf[MACDFunction], _baseSer, baseVar, periodSlow, periodFast).macd(sessionId, idx)
    }
    
    protected def mfi(idx:Int, period:Factor) :Float = {
        getInstance(classOf[MFIFunction], _baseSer, period).mfi(sessionId, idx)
    }
    
    protected def mtm(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[MTMFunction], _baseSer, baseVar, period).mtm(sessionId, idx)
    }
    
    protected def obv(idx:Int) :Float = {
        getInstance(classOf[OBVFunction], _baseSer).obv(sessionId, idx)
    }
    
    protected def roc(idx:Int, baseVar:Var[_], period:Factor) :Float = {
        getInstance(classOf[ROCFunction], _baseSer, baseVar, period).roc(sessionId, idx)
    }
    
    protected def rsi(idx:Int, period:Factor) :Float = {
        getInstance(classOf[RSIFunction], _baseSer, period).rsi(sessionId, idx)
    }
    
    protected def sar(idx:Int, initial:Factor, step:Factor, maximum:Factor) :Float = {
        getInstance(classOf[SARFunction], _baseSer, initial, step, maximum).sar(sessionId, idx)
    }
    
    protected def sarDirection(idx:Int, initial:Factor, step:Factor, maximum:Factor) :Direction = {
        getInstance(classOf[SARFunction], _baseSer, initial, step, maximum).sarDirection(sessionId, idx)
    }
    
    protected def stochK(idx:Int, period:Factor, periodK:Factor) :Float = {
        getInstance(classOf[STOCHKFunction], _baseSer, period, periodK).stochK(sessionId, idx)
    }
    
    protected def stochD(idx:Int, period:Factor, periodK:Factor, periodD:Factor) :Float = {
        getInstance(classOf[STOCHDFunction], _baseSer, period, periodK, periodD).stochD(sessionId, idx)
    }
    
    protected def stochJ(idx:Int, period:Factor, periodK:Factor, periodD:Factor) :Float = {
        getInstance(classOf[STOCHJFunction], _baseSer, period, periodK, periodD).stochJ(sessionId, idx)
    }
    
    protected def wms(idx:Int, period:Factor) :Float = {
        getInstance(classOf[WMSFunction], _baseSer, period).wms(sessionId, idx)
    }
    
    protected def zigzag(idx:Int, percent:Factor) :Float = {
        getInstance(classOf[ZIGZAGFunction], _baseSer, percent).zigzag(sessionId, idx)
    }
    
    protected def pseudoZigzag(idx:Int, percent:Factor) :Float = {
        getInstance(classOf[ZIGZAGFunction], _baseSer, percent).pseudoZigzag(sessionId, idx)
    }
    
    protected def zigzagDirection(idx:Int, percent:Factor) :Direction = {
        getInstance(classOf[ZIGZAGFunction], _baseSer, percent).zigzagDirection(sessionId, idx)
    }
    
    def dispose :Unit = {
        computableHelper.dispose
    }
    
    /**
     * ----------------------------------------------------------------------
     * End of Functions
     */
    
    
    /**
     * Inner Fac class that will be added to AbstractIndicator instance
     * automaticlly when new it.
     * Fac can only lives in AbstractIndicator
     *
     *
     * @see addFac()
     * --------------------------------------------------------------------
     */
    object Factor {
        def apply(name:String, value:Number) = new InnerFactor(name, value)
        def apply(name:String, value:Number, step:Number) = new InnerFactor(name, value, step)
        def apply(name:String, value:Number, step:Number, minValue:Number, maxValue:Number) = new InnerFactor(name, value, step, minValue, maxValue)
    }
    
    protected class InnerFactor(name:String,
                                value:Number,
                                step:Number,
                                minValue:Number,
                                maxValue:Number) extends DefaultFactor(name, value, step, minValue, maxValue) {

        addFactor(this)

        def this(name:String, value:Number) = {
            this(name, value, null, null, null)
        }
        
        def this(name:String, value:Number, step:Number) = {
            this(name, value, step, null, null)
        }        
    }
}