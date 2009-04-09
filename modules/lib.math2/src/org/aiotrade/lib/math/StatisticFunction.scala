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
package org.aiotrade.lib.math

import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
object StatisticFunction {

    val MAX = 0
    val MIN = 1
    val VALUE = 0
    val MASS = 1

    def sum(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Float = {
        if (begIdx < 0 || endIdx >= values.size) {
            return Float.NaN
        }

        var sum = 0f
        for (i <- begIdx to endIdx) {
            val v = values(i)
            if (v != null) {  // @todo why this happens?
                sum += v
            }
        }

        sum
    }

    def isum(idx:Int, values:ArrayBuffer[Float], period:Int, prev:Float) :Float = {
        val lookbackIdx = lookback(idx, period)

        if (lookbackIdx < 0 || idx >= values.size) {
            Float.NaN
        } else if (lookbackIdx == 0) {
            /** compute first availabe sum (in case of enough period first time) */
            sum(values, 0, idx)
        } else {
            if (prev == Float.NaN) {
                /**
                 * although the 'values' size is enough, it may contains NaN
                 * element, thus cause the prevSum to be a NaN, we should
                 * precess this case by:
                 */
                sum(values, lookbackIdx, idx)
            } else {
                prev + values(idx) - values(lookbackIdx - 1)
            }
        }
    }

    def ma(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Float = {
        if (begIdx < 0 || endIdx >= values.size) {
            return Float.NaN
        }

        val period1 = period(begIdx, endIdx)
        sum(values, begIdx, endIdx) / period1
    }

    /**
     * ma(t + 1) = ma(t) + ( x(t) / N - x(t - n) / N )
     */
    def ima(idx:Int, values:ArrayBuffer[Float], period:Int, prev:Float) :Float = {
        val lookbackIdx = lookback(idx, period);

        if (lookbackIdx < 0 || idx >= values.size) {
            Float.NaN
        } else if (lookbackIdx == 0) {
            /** compute first availabe ma (in case of enough period first time) */
            ma(values, 0, idx);
        } else {
            if (prev == Float.NaN) {
                /**
                 * although the 'values' size is enough, it may contains NaN
                 * element, thus cause the prevSum to be a NaN, we should
                 * precess this case by:
                 */
                return ma(values, lookbackIdx, idx);
            } else {
                prev + (values(idx) - values(lookbackIdx - 1)) / (period * 1f)
            }
        }
    }

    def ema(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Float = {
        if (begIdx < 0 || endIdx >= values.size) {
            return Float.NaN
        }

        val period1 = period(begIdx, endIdx) * 1f
        val n = values.size
        var ema = 0f
        for (i <- begIdx to n if i < n) {
            ema += ((period1 - 1.0f) / (period1 + 1.0f)) * ema + (2.0f / (period1 + 1.0f)) * values(i)
        }

        ema
    }

    /**
     * ema(t + 1) = ema(t) + ( x(t) / N - ema(t) / N )
     *            = (1 - 1/N) * ema(t) + (1/N) * x(t)
     *            = (1 - a) * ema(t) + a * x(t)  // let a = 1/N
     */
    def iema(idx:Int, values:ArrayBuffer[Float], period:Int, prev:Float) :Float = {
        val value = values(idx) match {
            case x if x == Float.NaN => 0f
            case x => x
        }

        val a = 1f / (period * 1f)
        (1f - a) * prev + a * value
        //return ((period - 1.0f) / (period + 1.0f)) * prevEma + (2.0f / (period + 1.0f)) * value;
    }

    def max(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :float = {
        maxmin(values, begIdx, endIdx)(MAX)
    }

    def imax(idx:Int, values:ArrayBuffer[Float], period:Int, prev:Float) :Float = {
        val lookbackIdx = lookback(idx, period)

        if (lookbackIdx < 0 || idx >= values.size) {
            Float.NaN
        } else if (lookbackIdx == 0) {
            max(values, 0, idx);
        } else {
            if (prev == Float.NaN || values(lookbackIdx - 1) == prev) {
                max(values, lookbackIdx, idx)
            } else {
                val value = values(idx)
                if (prev >= value) prev else value
            }
        }
    }

    def min(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Float =  {
        maxmin(values, begIdx, endIdx)(MIN)
    }

    def imin(idx:Int, values:ArrayBuffer[Float], period:Int, prev:Float) :Float = {
        val lookbackIdx = lookback(idx, period)

        if (lookbackIdx < 0 || idx >= values.size) {
            Float.NaN
        } else if (lookbackIdx == 0) {
            min(values, 0, idx);
        } else {
            if (prev == Float.NaN || values(lookbackIdx - 1) == prev) {
                min(values, lookbackIdx, idx)
            } else {
                val value = values(idx)
                if (prev <= value) prev else value
            }
        }
    }

    def maxmin(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Array[Float] = {
        if (begIdx < 0) {
            return Array(Float.NaN, Float.NaN)
        }

        var max = -Float.MaxValue
        var min = +Float.MaxValue
        val lastIdx = Math.min(endIdx, values.size - 1)
        for (i <- begIdx to lastIdx) {
            val value = values(i)
            max = if (max >= value) max else value
            min = if (min <= value) min else value
        }

        Array(max, min)
    }

    def maxmin(values:Array[Float], begIdx:Int, endIdx:Int) :Array[Float] = {
        if (begIdx < 0) {
            return Array(Float.NaN, Float.NaN)
        }

        var max = -Float.MaxValue
        var min = +Float.MaxValue
        val lastIdx = Math.min(endIdx, values.length - 1)
        for (i <- begIdx to lastIdx) {
            val value = values(i)
            max = if (max >= value) max else value
            min = if (min <= value) min else value
        }

        Array(max, min)
    }

    /**
     * Standard Deviation
     */
    def stdDev(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int) :Float = {
        if (begIdx < 0 || endIdx >= values.size) {
            return Float.NaN
        }

        val ma1 = ma(values, begIdx, endIdx)
        val lastIdx = Math.min(endIdx, values.size - 1)
        var deviation_square_sum = 0d
        for (i <- begIdx to lastIdx) {
            val deviation = values(i) - ma1
            deviation_square_sum += deviation * deviation
        }

        val period1 = period(begIdx, endIdx) * 1d
        Math.sqrt(deviation_square_sum / period1).asInstanceOf[Float]
    }

    /**
     * Probability Mass Function
     */
    def probMass(values:ArrayBuffer[Float], begIdx:Int, endIdx:Int, nIntervals:Int) :Array[Array[Float]] = {
        probMass(values, null, begIdx, endIdx, nIntervals)
    }

    /**
     * Probability Mass Function
     */
    def probMass(values:ArrayBuffer[Float], weights:ArrayBuffer[Float],
                 begIdx:Int, endIdx:int, nIntervals:Int) :Array[Array[Float]] = {

        if (nIntervals <= 0) {
            return null;
        }

        val begIdx1 = if (begIdx < 0) 0 else begIdx

        val maxmin1 = maxmin(values, begIdx1, endIdx);
        val max = maxmin1(MAX)
        val min = maxmin1(MIN)
        probMass(values, weights, begIdx1, endIdx, max, min, nIntervals)
    }

    /**
     * Probability Density Function
     */
    def probMass(values:ArrayBuffer[Float],
                 begIdx:Int, endIdx:Int, interval:Double) :Array[Array[Float]] = {

        probMass(values, null, begIdx, endIdx, interval)
    }

    /**
     * Probability Mass Function
     */
    def probMass(values:ArrayBuffer[Float], weights:ArrayBuffer[Float],
                 begIdx:Int, endIdx:Int, interval:Double) :Array[Array[Float]] = {

        if (interval <= 0) {
            return null
        }

        val begIdx1 = if (begIdx < 0) 0 else begIdx

        val maxmin1 = maxmin(values, begIdx1, endIdx);
        val max = maxmin1(MAX)
        val min = maxmin1(MIN)
        val nIntervals = (((max - min) / interval) + 1).asInstanceOf[Int]
        probMass(values, weights, begIdx1, endIdx, max, min, nIntervals)
    }

    /**
     * Probability Mass Function
     */
    private def probMass(values:ArrayBuffer[Float], weights:ArrayBuffer[Float],
                         begIdx:Int, endIdx:Int, max:Float, min:Float, nIntervals:Int) :Array[Array[Float]] = {

        if (nIntervals <= 0) {
            return null
        }

        val begIdx1 = if (begIdx < 0) 0 else begIdx

        val interval = (max - min) / ((nIntervals - 1) * 1f)
        val mass = new Array[Array[Float]](2)
        mass.map{x => new Array[Float](nIntervals)}
        for (i <- 0 until nIntervals) {
            mass(VALUE)(i) = min + i * interval
            mass(MASS)(i) = 0f
        }

        val lastIdx = Math.min(endIdx, values.size - 1)
        var total = 0f
        for (i <- begIdx1 to lastIdx) {
            val value = values(i)
            val weight = if (weights == null) 1f else weights(i).floatValue
            if (value >= min && value <= max) {
                /** only calculate those between max and min */
                val densityIdx = ((value - min) / interval).asInstanceOf[Int]
                mass(MASS)(densityIdx) += weight
            }

            total += weight
        }

        mass(MASS).map{x => x / total}

        mass
    }

    /**
     * Probability Density Function
     */
    def probMassWithTimeInfo(values:ArrayBuffer[Float], weights:ArrayBuffer[Float],
                             begIdx:Int, endIdx:Int, interval:Float) :Array[Array[Float]] = {

        if (begIdx < 0 || interval <= 0) {
            return null
        }

        val maxmin1 = maxmin(values, begIdx, endIdx)
        val max = maxmin1(MAX)
        val min = maxmin1(MIN)
        val nIntervals = (((max - min) / interval) + 1).asInstanceOf[Int]
        val period1 = period(begIdx, endIdx)
        val mass = new Array[Array[Float]](2)
        mass.map{x => new Array[Float](nIntervals)}
        for (i <- 0 until nIntervals) {
            mass(VALUE)(i) = min + i * interval
            mass(MASS)(i) = 0f
        }

        val lastIdx = Math.min(endIdx, values.size - 1)
        var total = 0f
        for (i <- begIdx to lastIdx) {
            val value = values(i)
            val weight = if (weights == null) 1f else weights(i)
            if (value >= min && value <= max) {
                /** only calculate those between max and min */
                val densityIdx = ((value - min) / interval).asInstanceOf[Int]
                mass(MASS)(densityIdx) += weight
            }

            total += weight
        }

        mass(MASS).map{x => x / total}

        mass
    }

    private def period(begIdx:Int, endIdx:Int) :Int = {
        endIdx - begIdx + 1
    }

    private def lookback(idx:Int, period:Int) :Int = {
        idx - period + 1
    }
}

