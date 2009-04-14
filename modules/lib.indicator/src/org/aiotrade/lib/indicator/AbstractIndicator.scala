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
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.computable.DefaultOption;
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
//@IndicatorName("Abstract Indicator")
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
        AbstractFunction.getInstance(clazz, baseSer, args)
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
     * inject DefaultOpt(s): new DefaultOpt() will call addOpt which delegated
     * by computableHelper.addOpt(..)
     */
    private val computableHelper = new ComputableHelper
    
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
    /** base series' item size */
    protected var _itemSize :Int = _
    
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
        
            this.computableHelper.init(baseSer, this)
        
            initPredefinedVarsOfBaseSer
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
    
    protected def addOpt(opt:Opt) : Unit = {
        computableHelper.addOpt(opt)
    }
    
    def opts :ArrayBuffer[Opt] = {
        computableHelper.opts
    }
    
    def opts_=(opts:ArrayBuffer[Opt]) :Unit = {
        computableHelper.opts = opts
    }
    
    def opts_=(values:Array[Number]) :Unit = {
        computableHelper.opts = values
    }
    
    def grids :Array[Float] = _grids
    
    def isOverlapping :Boolean = _overlapping
    def overlapping_=(b:Boolean) = {
        _overlapping = b
    }
    
    def computedTime :Long = {
        lastOccurredTime
    }
    
    /**
     * !NOTICE
     * It's better to fire ser change events or opt change event instead of
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
     * @param begin time to be computed
     */
    def computeFrom(begTime:Long) :Unit = {
        setSessionId
        
        /**
         * get baseSer's itemList size via protected _itemSize here instead of by
         * indicator's subclass when begin computeCont, because we could not
         * sure if the baseSer's _itemSize size has been change by others
         * (DataServer etc.)
         */
        _itemSize = _baseSer.itemList.size
        
        val begIdx = computableHelper.preComputeFrom(begTime)
        /** fill with clear items from begIdx: */
        var i = begIdx; while (i < _itemSize) {
            val time = _baseSer.timestamps(i)
            
            /**
             * if baseSer is MasterSer, we'll use timeOfRow(idx) to get the time,
             * this enable returning a good time even idx < 0 or exceed itemList.size()
             * because it will trace back in *calendar* time.
             * @TODO
             */
            /*-
             long time = _baseSer instanceof MasterSer ?
             ((MasterSer)_baseSer).timeOfRow(i) :
             _baseSer.timeOfIndex(i);
             */
            
            /** 
             * we've fetch time from _baseSer, but not sure if this time has been 
             * added to my timestamps, so, do any way:
             */
            createItemOrClearIt(time)
            i += 1
        }
        
        computeCont(begIdx)
        
        computableHelper.postComputeFrom
    }
    
    protected def preComputeFrom(begTime:Long) :Int = {
        computableHelper.preComputeFrom(begTime)
    }
    
    def postComputeFrom :Unit = {
        computableHelper.postComputeFrom
    }
    
    protected def computeCont(begIdx:Int) :Unit
    
    protected def longDescription :String = {
        _lname;
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
    
    protected def sum(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[SUMFunction], _baseSer, var1, period).sum(sessionId, idx);
    }
    
    protected def max(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[MAXFunction], _baseSer, var1, period).max(sessionId, idx);
    }
    
    protected def min(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[MINFunction], _baseSer, var1, period).min(sessionId, idx);
    }
    
    protected def ma(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[MAFunction], _baseSer, var1, period).ma(sessionId, idx);
    }
    
    protected def ema(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[EMAFunction], _baseSer, var1, period).ema(sessionId, idx);
    }
    
    protected def stdDev(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[STDDEVFunction], _baseSer, var1, period).stdDev(sessionId, idx);
    }
    
    protected def probMass(idx:Int, var1:Var[Float], period:Opt, nInterval:Opt) :Array[Array[Float]] = {
        return getInstance(classOf[PROBMASSFunction], _baseSer, var1, null, period, nInterval).probMass(sessionId, idx)
    }
    
    protected def probMass(idx:Int, var1:Var[Float], weight:Var[Float], period:Opt, nInterval:Opt) :Array[Array[Float]] = {
        return getInstance(classOf[PROBMASSFunction], _baseSer, var1, weight, period, nInterval).probMass(sessionId, idx)
    }
    
    protected def tr(idx:Int) :Float = {
        return getInstance(classOf[TRFunction], _baseSer).tr(sessionId, idx);
    }
    
    protected def dmPlus(idx:Int) :Float = {
        return getInstance(classOf[DMFunction], _baseSer).dmPlus(sessionId, idx);
    }
    
    protected def dmMinus(idx:Int) :Float = {
        return getInstance(classOf[DMFunction], _baseSer).dmMinus(sessionId, idx);
    }
    
    protected def diPlus(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[DIFunction], _baseSer, period).diPlus(sessionId, idx);
    }
    
    protected def diMinus(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[DIFunction], _baseSer, period).diMinus(sessionId, idx);
    }
    
    protected def dx(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[DXFunction], _baseSer, period).dx(sessionId, idx);
    }
    
    protected def adx(idx:Int, periodDi:Opt, periodAdx:Opt) :Float = {
        return getInstance(classOf[ADXFunction], _baseSer, periodDi, periodAdx).adx(sessionId, idx);
    }
    
    protected def adxr(idx:Int, periodDi:Opt, periodAdx:Opt) :Float = {
        return getInstance(classOf[ADXRFunction], _baseSer, periodDi, periodAdx).adxr(sessionId, idx);
    }
    
    protected def bollMiddle(idx:Int, var1:Var[_], period:Opt, alpha:Opt) :Float = {
        return getInstance(classOf[BOLLFunction], _baseSer, var1, period, alpha).bollMiddle(sessionId, idx);
    }
    
    protected def bollUpper(idx:Int, var1:Var[_], period:Opt, alpha:Opt) :Float = {
        return getInstance(classOf[BOLLFunction], _baseSer, var1, period, alpha).bollUpper(sessionId, idx);
    }
    
    protected def bollLower(idx:Int, var1:Var[_], period:Opt, alpha:Opt) :Float = {
        return getInstance(classOf[BOLLFunction], _baseSer, var1, period, alpha).bollLower(sessionId, idx);
    }
    
    protected def cci(idx:Int, period:Opt, alpha:Opt) :Float = {
        return getInstance(classOf[CCIFunction], _baseSer, period, alpha).cci(sessionId, idx);
    }
    
    protected def macd(idx:Int, var1:Var[_], periodSlow:Opt, periodFast:Opt) :Float = {
        return getInstance(classOf[MACDFunction], _baseSer, var1, periodSlow, periodFast).macd(sessionId, idx);
    }
    
    protected def mfi(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[MFIFunction], _baseSer, period).mfi(sessionId, idx);
    }
    
    protected def mtm(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[MTMFunction], _baseSer, var1, period).mtm(sessionId, idx);
    }
    
    protected def obv(idx:Int) :Float = {
        return getInstance(classOf[OBVFunction], _baseSer).obv(sessionId, idx);
    }
    
    protected def roc(idx:Int, var1:Var[_], period:Opt) :Float = {
        return getInstance(classOf[ROCFunction], _baseSer, var1, period).roc(sessionId, idx);
    }
    
    protected def rsi(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[RSIFunction], _baseSer, period).rsi(sessionId, idx);
    }
    
    protected def sar(idx:Int, initial:Opt, step:Opt, maximum:Opt) :Float = {
        return getInstance(classOf[SARFunction], _baseSer, initial, step, maximum).sar(sessionId, idx);
    }
    
    protected def sarDirection(idx:Int, initial:Opt, step:Opt, maximum:Opt) :Direction = {
        return getInstance(classOf[SARFunction], _baseSer, initial, step, maximum).sarDirection(sessionId, idx);
    }
    
    protected def stochK(idx:Int, period:Opt, periodK:Opt) :Float = {
        return getInstance(classOf[STOCHKFunction], _baseSer, period, periodK).stochK(sessionId, idx);
    }
    
    protected def stochD(idx:Int, period:Opt, periodK:Opt, periodD:Opt) :Float = {
        return getInstance(classOf[STOCHDFunction], _baseSer, period, periodK, periodD).stochD(sessionId, idx);
    }
    
    protected def stochJ(idx:Int, period:Opt, periodK:Opt, periodD:Opt) :Float = {
        return getInstance(classOf[STOCHJFunction], _baseSer, period, periodK, periodD).stochJ(sessionId, idx);
    }
    
    protected def wms(idx:Int, period:Opt) :Float = {
        return getInstance(classOf[WMSFunction], _baseSer, period).wms(sessionId, idx);
    }
    
    protected def zigzag(idx:Int, percent:Opt) :Float = {
        return getInstance(classOf[ZIGZAGFunction], _baseSer, percent).zigzag(sessionId, idx);
    }
    
    protected def pseudoZigzag(idx:Int, percent:Opt) :Float = {
        return getInstance(classOf[ZIGZAGFunction], _baseSer, percent).pseudoZigzag(sessionId, idx);
    }
    
    protected def zigzagDirection(idx:Int, percent:Opt) :Direction = {
        return getInstance(classOf[ZIGZAGFunction], _baseSer, percent).zigzagDirection(sessionId, idx);
    }
    
    def dispose :Unit = {
        computableHelper.dispose
    }
    
    /**
     * ----------------------------------------------------------------------
     * End of Functions
     */
    
    
    /**
     * Inner DefaultOpt class that will be added to AbstractIndicator instance
     * automaticlly when new it.
     * DefaultOpt can only lives in AbstractIndicator
     *
     *
     * @see addOpt()
     * --------------------------------------------------------------------
     */
    class DefaultOpt(name:String,
                     value:Number,
                     step:Number,
                     minValue:Number,
                     maxValue:Number) extends DefaultOption(name, value, step, minValue, maxValue) {

        addOpt(this)

        def this(name:String, value:Number) = {
            this(name, value, null, null, null)
        }
        
        def this(name:String, value:Number, step:Number) = {
            this(name, value, step, null, null)
        }        
    }
}