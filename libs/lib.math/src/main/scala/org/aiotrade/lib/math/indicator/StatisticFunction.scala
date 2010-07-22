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

import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.collection.ArrayList

/**
 *
 * @author Caoyuan Deng
 */
object StatisticFunction {

  val MAX = 0
  val MIN = 1
  val VALUE = 0
  val MASS = 1

  final def sum(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double = {
    if (begIdx < 0 || endIdx >= values.size) {
      return Null.Double
    }

    var sum = 0.0
    var i = begIdx
    while (i <= endIdx) {
      val value = values(i)
      if (Null.not(value)) {
        sum += value
      }
      i += 1
    }

    sum
  }

  final def isum(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.size) {
      Null.Double
    } else if (lookbackIdx == 0) {
      /** compute first availabe sum (in case of enough period first time) */
      sum(values, 0, idx)
    } else {
      if (Null.is(prev)) {
        /**
         * although the 'values' size is enough, it may contains Null.Double
         * element, thus cause the prevSum to be a Null.Double, we should
         * precess this case by:
         */
        sum(values, lookbackIdx, idx)
      } else {
        prev + values(idx) - values(lookbackIdx - 1)
      }
    }
  }

  final def ma(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double = {
    if (begIdx < 0 || endIdx >= values.size) {
      return Null.Double
    }

    val period1 = period(begIdx, endIdx)
    sum(values, begIdx, endIdx) / period1
  }

  /**
   * ma(t + 1) = ma(t) + ( x(t) / N - x(t - n) / N )
   */
  final def ima(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.size) {
      Null.Double
    } else if (lookbackIdx == 0) {
      /** compute first available ma (in case of enough period first time) */
      ma(values, 0, idx)
    } else {
      if (Null.is(prev)) {
        /**
         * although the 'values' size is enough, it may contains Null.Double
         * element, thus cause the prevSum to be a Null.Double, we should
         * precess this case by:
         */
        ma(values, lookbackIdx, idx)
      } else {
        prev + (values(idx) - values(lookbackIdx - 1)) / (period * 1f)
      }
    }
  }

  final def ema(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double = {
    if (begIdx < 0 || endIdx >= values.size) {
      return Null.Double
    }

    val period1 = period(begIdx, endIdx) * 1.0
    var ema = 0.0
    var i = begIdx
    while (i <= endIdx) {
      ema += ((period1 - 1.0) / (period1 + 1.0)) * ema + (2.0 / (period1 + 1.0)) * values(i)
      i += 1
    }

    ema
  }

  /**
   * ema(t + 1) = ema(t) + ( x(t) / N - ema(t) / N )
   *            = (1 - 1/N) * ema(t) + (1/N) * x(t)
   *            = (1 - a) * ema(t) + a * x(t)  // let a = 1/N
   */
  final def iema(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value)) value = 0.0


    /** @todo */
    if (Null.is(prev)) {
      0F
    } else {
      val a = 1.0 / (period * 1.0)
      (1.0 - a) * prev + a * value
    }
    //return ((period - 1.0f) / (period + 1.0f)) * prevEma + (2.0f / (period + 1.0f)) * value;
  }

  final def max(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double = {
    maxmin(values, begIdx, endIdx)(MAX)
  }

  final def imax(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.size) {
      Null.Double
    } else if (lookbackIdx == 0) {
      max(values, 0, idx)
    } else {
      if (Null.is(prev) || values(lookbackIdx - 1) == prev) {
        max(values, lookbackIdx, idx)
      } else {
        val value = values(idx)
        if (prev >= value) prev else value
      }
    }
  }

  final def min(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double =  {
    maxmin(values, begIdx, endIdx)(MIN)
  }

  final def imin(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.size) {
      Null.Double
    } else if (lookbackIdx == 0) {
      min(values, 0, idx)
    } else {
      if (Null.is(prev) || values(lookbackIdx - 1) == prev) {
        min(values, lookbackIdx, idx)
      } else {
        val value = values(idx)
        if (prev <= value) prev else value
      }
    }
  }

  final def maxmin(values: ArrayList[Double], begIdx: Int, endIdx: Int): Array[Double] = {
    if (begIdx < 0) {
      return Array(Null.Double, Null.Double)
    }

    var max = Double.MinValue
    var min = Double.MaxValue
    val lastIdx = math.min(endIdx, values.size - 1)
    var i = begIdx
    while (i <= lastIdx) {
      val value = values(i)
      if (Null.not(value)) {
        max = if (max >= value) max else value
        min = if (min <= value) min else value
      }
      i += 1
    }

    Array(max, min)
  }

  final def maxmin(values: Array[Double], begIdx: Int, endIdx: Int): Array[Double] = {
    if (begIdx < 0) {
      return Array(Null.Double, Null.Double)
    }

    var max = Double.MinValue
    var min = Double.MaxValue
    val lastIdx = math.min(endIdx, values.length - 1)
    var i = begIdx
    while (i <= lastIdx) {
      val value = values(i)
      if (Null.not(value)) {
        max = math.max(max, value)
        min = math.min(min, value)
      }
      i += 1
    }

    Array(max, min)
  }

  /**
   * Standard Deviation
   */
  final def stdDev(values: ArrayList[Double], begIdx: Int, endIdx: Int): Double = {
    if (begIdx < 0 || endIdx >= values.size) {
      return Null.Double
    }

    val ma1 = ma(values, begIdx, endIdx)
    val lastIdx = math.min(endIdx, values.size - 1)
    var deviation_square_sum = 0.0
    var i = begIdx
    while (i <= lastIdx) {
      val deviation = values(i) - ma1
      deviation_square_sum += deviation * deviation
      i += 1
    }

    val period1 = period(begIdx, endIdx) * 1.0
    math.sqrt(deviation_square_sum / period1)
  }

  /**
   * Probability Mass Function
   */
  final def probMass(values: ArrayList[Double], begIdx: Int, endIdx: Int, nIntervals: Int): Array[Array[Double]] = {
    probMass(values, null, begIdx, endIdx, nIntervals)
  }

  /**
   * Probability Mass Function
   */
  final def probMass(values: ArrayList[Double], weights: ArrayList[Double],
                     begIdx: Int, endIdx: Int, nIntervals: Int
  ): Array[Array[Double]] = {

    if (nIntervals <= 0) {
      return null
    }

    val begIdx1 = if (begIdx < 0) 0 else begIdx

    val maxmin1 = maxmin(values, begIdx1, endIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    probMass(values, weights, begIdx1, endIdx, max, min, nIntervals)
  }

  /**
   * Probability Density Function
   */
  final def probMass(values: ArrayList[Double],
                     begIdx: Int, endIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    probMass(values, null, begIdx, endIdx, interval)
  }

  /**
   * Probability Mass Function
   */
  final def probMass(values: ArrayList[Double], weights: ArrayList[Double],
                     begIdx: Int, endIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    if (interval <= 0) {
      return null
    }

    val begIdx1 = if (begIdx < 0) 0 else begIdx

    val maxmin1 = maxmin(values, begIdx1, endIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    val nIntervals = (((max - min) / interval) + 1).toInt
    probMass(values, weights, begIdx1, endIdx, max, min, nIntervals)
  }

  /**
   * Probability Mass Function
   */
  private def probMass(values: ArrayList[Double], weights: ArrayList[Double],
                       begIdx: Int, endIdx: Int, max: Double, min: Double, nIntervals: Int
  ): Array[Array[Double]] = {

    if (nIntervals <= 0) {
      return null
    }

    val begIdx1 = if (begIdx < 0) 0 else begIdx

    val interval = (max - min) / ((nIntervals - 1) * 1.0)
    val mass = new Array[Array[Double]](2, nIntervals)
    var i = 0
    while (i < nIntervals) {
      mass(VALUE)(i) = min + i * interval
      mass(MASS)(i) = 0.0
      i += 1
    }

    val lastIdx = math.min(endIdx, values.size - 1)
    var total = 0.0
    i = begIdx1
    while (i <= lastIdx) {
      val value = values(i)
      val weight = if (weights == null) 1.0 else weights(i)
      if (value >= min && value <= max) {
        /** only calculate those between max and min */
        val densityIdx = ((value - min) / interval).toInt
        mass(MASS)(densityIdx) += weight
      }

      total += weight
      i += 1
    }

    mass(MASS) map {_ / total}

    mass
  }

  /**
   * Probability Density Function
   */
  final def probMassWithTimeInfo(values: ArrayList[Double], weights: ArrayList[Double],
                                 begIdx: Int, endIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    if (begIdx < 0 || interval <= 0) {
      return null
    }

    val maxmin1 = maxmin(values, begIdx, endIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    val nIntervals = (((max - min) / interval) + 1).toInt
    val period1 = period(begIdx, endIdx)
    val mass = new Array[Array[Double]](2, nIntervals)
    var i = 0
    while (i < nIntervals) {
      mass(VALUE)(i) = min + i * interval
      mass(MASS) (i) = 0.0
      i += 1
    }

    val lastIdx = math.min(endIdx, values.size - 1)
    var total = 0.0
    i = begIdx
    while (i <= lastIdx) {
      val value = values(i)
      val weight = if (weights == null) 1f else weights(i)
      if (value >= min && value <= max) {
        /** only calculate those between max and min */
        val densityIdx = ((value - min) / interval).toInt
        mass(MASS)(densityIdx) += weight
      }

      total += weight
      i += 1
    }

    mass(MASS) map {_ / total}

    mass
  }

  private def period(begIdx: Int, endIdx: Int): Int = {
    endIdx - begIdx + 1
  }

  private def lookback(idx: Int, period: Int): Int = {
    idx - period + 1
  }
}

