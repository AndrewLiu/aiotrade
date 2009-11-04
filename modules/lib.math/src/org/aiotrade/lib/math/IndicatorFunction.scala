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

import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.util.collection.ArrayList

/**
 *
 * @author Caoyuan Deng
 */
object IndicatorFunction {
    
  final def dmPlus(idx: Int, highs: ArrayList[Float], lows: ArrayList[Float]): Float = {
    if (idx == 0) {
            
      Null.Float
            
    } else {
            
      if (highs(idx) > highs(idx - 1) && lows(idx) > lows(idx - 1)) {
        highs(idx) - highs(idx - 1)
      } else if (highs(idx) < highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        0f
      } else if (highs(idx) > highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        if (highs(idx) - highs(idx - 1) > lows(idx - 1) - lows(idx)) {
          highs(idx) - highs(idx - 1)
        } else {
          0f
        }
      } else if (highs(idx) <  highs(idx - 1) && lows(idx) >  lows(idx - 1)) {
        0f
      } else if (highs(idx) == highs(idx - 1) && lows(idx) == lows(idx - 1)) {
        0f
      } else if (lows(idx) > highs(idx - 1)) {
        highs(idx) - highs(idx)
      } else if (highs(idx) < lows(idx - 1)) {
        0f
      } else {
        0f
      }
            
    }
  }
    
  final def dmMinus(idx: Int, highs: ArrayList[Float], lows: ArrayList[Float]): Float = {
    if (idx == 0) {
            
      Null.Float
            
    } else {
            
      if (highs(idx) > highs(idx - 1) && lows(idx) > lows(idx - 1)) {
        0f
      } else if (highs(idx) < highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        lows(idx - 1) - lows(idx)
      } else if (highs(idx) > highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        if (highs(idx) - highs(idx - 1) > lows(idx - 1) - lows(idx)) {
          0f
        } else {
          lows(idx - 1) - lows(idx)
        }
      } else if (highs(idx) <  highs(idx - 1) && lows(idx) >  lows(idx - 1)) {
        0f
      } else if (highs(idx) == highs(idx - 1) && lows(idx) == lows(idx - 1)) {
        0f
      } else if (lows(idx) > highs(idx - 1)) {
        0f
      } else if (highs(idx) < lows(idx - 1)) {
        lows(idx - 1) - lows(idx)
      } else {
        0f
      }
            
    }
  }
    
  final def tr(idx: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx == 0) {
            
      Null.Float
            
    } else {
            
      val tr_tmp = Math.max(highs(idx) - lows(idx), Math.abs(highs(idx) - closes(idx - 1)))
      Math.max(tr_tmp, Math.abs(lows(idx) - closes(idx - 1)))
            
    }
  }
    
  final def diPlus(idx: Int, period: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx < period - 1) {
            
      Null.Float
            
    } else {
            
      val dms = new ArrayList[Float]
      val trs = new ArrayList[Float]
            
      val fromIdx = idx - (period - 1)
      val toIdx   = idx

      var i = fromIdx
      while (i <= toIdx) {
                
        dms += dmPlus(i, highs, lows)
        trs += tr(i, highs, lows, closes)

        i += 1
      }
            
      val ma_dm = StatisticFunction.ma(dms, 0, period - 1)
      val ma_tr = StatisticFunction.ma(trs, 0, period - 1)
            
      if (ma_tr == 0) 0 else ma_dm / ma_tr * 100f
            
    }
  }
    
  final def diMinus(idx: Int, period: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx < period - 1) {
            
      Null.Float
            
    } else {
            
      val dms = new ArrayList[Float]
      val trs = new ArrayList[Float]
            
      val fromIdx = idx - (period - 1)
      val toIdx   = idx
            
      var i = fromIdx
      while (i <= toIdx) {
                
        dms += dmMinus(i, highs, lows)
        trs += tr(i, highs, lows, closes)

        i += 1
      }
            
      val ma_dm = StatisticFunction.ma(dms, 0, period - 1)
      val ma_tr = StatisticFunction.ma(trs, 0, period - 1)
            
      if (ma_tr == 0) 0 else ma_dm / ma_tr * 100f
            
    }
  }
    
  final def dx(idx: Int, period: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx < period - 1) {
            
      Null.Float
            
    } else {
            
      val diPlus1  = diPlus( idx, period, highs, lows, closes)
      val diMinus1 = diMinus(idx, period, highs, lows, closes)
            
      if (diPlus1 + diMinus1 == 0) 0 else Math.abs(diPlus1 - diMinus1) / (diPlus1 + diMinus1) * 100f
            
    }
  }
    
  final def adx(idx: Int, periodDI: Int, periodADX: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx < periodDI - 1 || idx < periodADX - 1) {
            
      Null.Float
            
    } else {
            
      val dxes = new ArrayList[Float]
            
      val fromIdx = idx - (periodADX - 1)
      val toIdx   = idx
            
      var i = fromIdx
      while (i <= toIdx) {
                
        dxes += dx(i, periodDI, highs, lows, closes)

        i += 1
      }
            
      StatisticFunction.ma(dxes, 0, periodADX - 1)
            
    }
  }
    
  final def adxr(idx: Int, periodDI: Int, periodADX: Int, highs: ArrayList[Float], lows: ArrayList[Float], closes: ArrayList[Float]): Float = {
    if (idx < periodDI - 1 || idx < periodADX - 1) {
            
      Null.Float
            
    } else {
            
      val adx1 = adx(idx,             periodDI, periodADX, highs, lows, closes)
      val adx2 = adx(idx - periodADX, periodDI, periodADX, highs, lows, closes)
            
      (adx1 + adx2) / 2f
            
    }
  }
    
}


