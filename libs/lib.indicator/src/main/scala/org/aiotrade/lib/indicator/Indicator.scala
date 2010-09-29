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

import org.aiotrade.lib.indicator.function._
import org.aiotrade.lib.math.indicator.DefaultFactor
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.indicator.Function
import org.aiotrade.lib.math.indicator.IndicatorHelper
import org.aiotrade.lib.math.timeseries.{DefaultTSer, TVar, BaseTSer}
import org.aiotrade.lib.securities.QuoteSer

/**
 *
 * @author Caoyuan Deng
 */
object Indicator {
  /** a static global session id */
  protected var sessionId: Long = _

  protected def setSessionId: Unit = {
    sessionId += 1
  }

  /**
   * a helper function for keeping the same functin form as Function, don't be
   * puzzled by the name, it actully will return funcion instance
   */
  protected def functionOf[T <: org.aiotrade.lib.math.indicator.Function](clazz: Class[T], baseSer: BaseTSer, args: Any*): T = {
    org.aiotrade.lib.math.indicator.Function.getInstance(clazz, baseSer, args: _*)
  }
}

import Indicator._
abstract class Indicator($baseSer: BaseTSer) extends DefaultTSer
                                                with org.aiotrade.lib.math.indicator.Indicator
                                                with IndicatorHelper
                                                with Ordered[Indicator] {

  /**
   * !NOTICE
   * IndicatorHelper should be created here, because it will be used to
   * inject Factor(s): new Factor() will call addFac which delegated
   * by indicatorHelper.addFac(..)
   */
  private var _computedTime = Long.MinValue
    
  /** some instance scope variables that can be set directly */
  protected var sname = "unkown"
  protected var lname = "unkown"

  /** base series to compute this */
  var baseSer: BaseTSer = _
    
  /** To store values of open, high, low, close, volume: */
  protected var O: TVar[Double] = _
  protected var H: TVar[Double] = _
  protected var L: TVar[Double] = _
  protected var C: TVar[Double] = _
  protected var V: TVar[Double] = _

  if ($baseSer != null) {
    set($baseSer)
  }
    
  /**
   * Make sure this null args contructor only be called and return instance to
   * NetBeans layer manager for register usage, so it just do nothing.
   */
  def this() = this(null)
    
  /**
   * make sure this method will be called before this instance return to any others:
   * 1. via constructor (except the no-arg constructor)
   * 2. via createInstance
   */
  def set(baseSer: BaseTSer) {
    if (baseSer != null) {
      super.set(baseSer.freq)
      super.setBaseSer(baseSer)

      initPredefinedVarsOfBaseSer
    }
  }
    
  /** override this method to define your predefined vars */
  protected def initPredefinedVarsOfBaseSer {
    baseSer match {
      case x: QuoteSer =>
        O = x.open
        H = x.high
        L = x.low
        C = x.close
        V = x.volume
      case _ =>
    }
  }
        
  def computedTime: Long = _computedTime
    
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
  def computeFrom(fromTime: Long) {
    setSessionId

    try {
      timestamps.readLock.lock

      val fromIdx = super.preComputeFrom(fromTime)
      /**
       * @Note
       * It's better to pass Size as param to computeCont instead of keep it as instance field,
       * so, we do not need to worry about if field _Size will be changed concurrent by another
       * thread
       */
      val size = timestamps.size

      computeCont(fromIdx, size)
        
      _computedTime = timestamps.lastOccurredTime
      super.postComputeFrom
      
    } finally {
      timestamps.readLock.unlock
    }
  }
        
  protected def computeCont(fromIdx: Int, size: Int): Unit
    
  protected def longDescription: String = lname
    
  override def shortDescription: String = sname
  override def shortDescription_=(description: String) {
    this.sname = description
  }

  def displayName = shortDescription + " - (" + longDescription + ")"
    
  def compare(another: Indicator): Int = {
    if (this.shortDescription.equalsIgnoreCase(another.shortDescription)) {
      if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
    } else {
      this.shortDescription.compareTo(another.shortDescription)
    }
  }
    
  def createNewInstance(baseSer: BaseTSer): Indicator = {
    try {
      val instance = this.getClass.newInstance.asInstanceOf[Indicator]
      instance.set(baseSer)
      instance
    } catch {
      case ex: IllegalAccessException => ex.printStackTrace; null
      case ex: InstantiationException => ex.printStackTrace; null
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

  // ----- Functions for test
  final protected def crossOver(idx: Int, var1: TVar[Double], var2: TVar[Double]): Boolean = {
    if (idx > 0) {
      if (var1(idx) >= var2(idx) &&
          var1(idx - 1) < var2(idx - 1)) {
        return true
      }
    }
    false
  }

  final protected def crossOver(idx: Int, var1: TVar[Double], value:Double): Boolean = {
    if (idx > 0) {
      if (var1(idx) >= value &&
          var1(idx - 1) < value) {
        return true
      }
    }
    false
  }

  final protected def crossUnder(idx: Int, var1: TVar[Double], var2: TVar[Double]): Boolean = {
    if (idx > 0) {
      if (var1(idx) < var2(idx) &&
          var1(idx - 1) >= var2(idx - 1)) {
        return true
      }
    }
    false
  }

  final protected def crossUnder(idx: Int, var1: TVar[Double], value: Double): Boolean = {
    if (idx > 0) {
      if (var1(idx) < value &&
          var1(idx - 1) >= value) {
        true
      }
    }
    false
  }

  final protected def turnUp(idx: Int, var1: TVar[Double]): Boolean = {
    if (idx > 1) {
      if (var1(idx) > var1(idx - 1) &&
          var1(idx - 1) <= var1(idx - 2)) {
        return true
      }
    }
    false
  }

  final protected def turnDown(idx: Int, var1: TVar[Double]): Boolean = {
    if (idx > 1) {
      if (var1(idx) < var1(idx - 1) &&
          var1(idx - 1) >= var1(idx - 2)) {
        return true
      }
    }
    false
  }

  // ----- End of functions for test
    
  final protected def sum(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[SUMFunction], baseSer, baseVar, period).sum(sessionId, idx)
  }
    
  final protected def max(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[MAXFunction], baseSer, baseVar, period).max(sessionId, idx)
  }
    
  final protected def min(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[MINFunction], baseSer, baseVar, period).min(sessionId, idx)
  }
    
  final protected def ma(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[MAFunction], baseSer, baseVar, period).ma(sessionId, idx)
  }
    
  final protected def ema(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[EMAFunction], baseSer, baseVar, period).ema(sessionId, idx)
  }
    
  final protected def stdDev(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[STDDEVFunction], baseSer, baseVar, period).stdDev(sessionId, idx)
  }
    
  final protected def probMass(idx: Int, baseVar: TVar[Double], period: Factor, nInterval: Factor): Array[Array[Double]] = {
    functionOf(classOf[PROBMASSFunction], baseSer, baseVar, null, period, nInterval).probMass(sessionId, idx)
  }
    
  final protected def probMass(idx: Int, baseVar: TVar[Double], weight: TVar[Double], period: Factor, nInterval: Factor): Array[Array[Double]] = {
    functionOf(classOf[PROBMASSFunction], baseSer, baseVar, weight, period, nInterval).probMass(sessionId, idx)
  }
    
  final protected def tr(idx: Int): Double = {
    functionOf(classOf[TRFunction], baseSer).tr(sessionId, idx)
  }
    
  final protected def dmPlus(idx: Int): Double = {
    functionOf(classOf[DMFunction], baseSer).dmPlus(sessionId, idx)
  }
    
  final protected def dmMinus(idx: Int): Double = {
    functionOf(classOf[DMFunction], baseSer).dmMinus(sessionId, idx)
  }
    
  final protected def diPlus(idx: Int, period: Factor): Double = {
    functionOf(classOf[DIFunction], baseSer, period).diPlus(sessionId, idx)
  }
    
  final protected def diMinus(idx: Int, period: Factor): Double = {
    functionOf(classOf[DIFunction], baseSer, period).diMinus(sessionId, idx)
  }
    
  final protected def dx(idx: Int, period: Factor): Double = {
    functionOf(classOf[DXFunction], baseSer, period).dx(sessionId, idx)
  }
    
  final protected def adx(idx: Int, periodDi: Factor, periodAdx: Factor): Double = {
    functionOf(classOf[ADXFunction], baseSer, periodDi, periodAdx).adx(sessionId, idx)
  }
    
  final protected def adxr(idx: Int, periodDi: Factor, periodAdx: Factor): Double = {
    functionOf(classOf[ADXRFunction], baseSer, periodDi, periodAdx).adxr(sessionId, idx)
  }
    
  final protected def bollMiddle(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    functionOf(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollMiddle(sessionId, idx)
  }
    
  final protected def bollUpper(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    functionOf(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollUpper(sessionId, idx)
  }
    
  final protected def bollLower(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    functionOf(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollLower(sessionId, idx)
  }
    
  final protected def cci(idx: Int, period: Factor, alpha: Factor): Double = {
    functionOf(classOf[CCIFunction], baseSer, period, alpha).cci(sessionId, idx)
  }
    
  final protected def macd(idx: Int, baseVar: TVar[_], periodSlow: Factor, periodFast: Factor): Double = {
    functionOf(classOf[MACDFunction], baseSer, baseVar, periodSlow, periodFast).macd(sessionId, idx)
  }
    
  final protected def mfi(idx: Int, period: Factor): Double = {
    functionOf(classOf[MFIFunction], baseSer, period).mfi(sessionId, idx)
  }
    
  final protected def mtm(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[MTMFunction], baseSer, baseVar, period).mtm(sessionId, idx)
  }
    
  final protected def obv(idx: Int): Double = {
    functionOf(classOf[OBVFunction], baseSer).obv(sessionId, idx)
  }
    
  final protected def roc(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    functionOf(classOf[ROCFunction], baseSer, baseVar, period).roc(sessionId, idx)
  }
    
  final protected def rsi(idx: Int, period: Factor): Double = {
    functionOf(classOf[RSIFunction], baseSer, period).rsi(sessionId, idx)
  }
    
  final protected def sar(idx: Int, initial: Factor, step: Factor, maximum: Factor): Double = {
    functionOf(classOf[SARFunction], baseSer, initial, step, maximum).sar(sessionId, idx)
  }
    
  final protected def sarDirection(idx: Int, initial: Factor, step: Factor, maximum: Factor): Direction = {
    functionOf(classOf[SARFunction], baseSer, initial, step, maximum).sarDirection(sessionId, idx)
  }
    
  final protected def stochK(idx: Int, period: Factor, periodK: Factor): Double = {
    functionOf(classOf[STOCHKFunction], baseSer, period, periodK).stochK(sessionId, idx)
  }
    
  final protected def stochD(idx: Int, period: Factor, periodK: Factor, periodD: Factor): Double = {
    functionOf(classOf[STOCHDFunction], baseSer, period, periodK, periodD).stochD(sessionId, idx)
  }
    
  final protected def stochJ(idx: Int, period: Factor, periodK: Factor, periodD: Factor): Double = {
    functionOf(classOf[STOCHJFunction], baseSer, period, periodK, periodD).stochJ(sessionId, idx)
  }
    
  final protected def wms(idx: Int, period: Factor): Double = {
    functionOf(classOf[WMSFunction], baseSer, period).wms(sessionId, idx)
  }
    
  final protected def zigzag(idx: Int, percent: Factor): Double = {
    functionOf(classOf[ZIGZAGFunction], baseSer, percent).zigzag(sessionId, idx)
  }
    
  final protected def pseudoZigzag(idx: Int, percent: Factor): Double = {
    functionOf(classOf[ZIGZAGFunction], baseSer, percent).pseudoZigzag(sessionId, idx)
  }
    
  final protected def zigzagDirection(idx: Int, percent: Factor): Direction = {
    functionOf(classOf[ZIGZAGFunction], baseSer, percent).zigzagDirection(sessionId, idx)
  }
    
  override def dispose {
    super.dispose
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
   * @see addFactor()
   * --------------------------------------------------------------------
   */
  object Factor {
    def apply(name: String, value: Double) =
      new InnerFactor(name, value, 1.0, Double.MinValue, Double.MaxValue)
    def apply(name: String, value: Double, step: Double) =
      new InnerFactor(name, value, step, Double.MinValue, Double.MaxValue)
    def apply(name: String, value: Double, step: Double, minValue: Double, maxValue: Double) =
      new InnerFactor(name, value, step, minValue, maxValue)
  }
    
  protected class InnerFactor(name: => String,
                              value: => Double,
                              step: => Double,
                              minValue: => Double,
                              maxValue: => Double
  ) extends DefaultFactor(name, value, step, minValue, maxValue) {

    addFactor(this)

  }
}
