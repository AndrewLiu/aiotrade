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
package org.aiotrade.math;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Caoyuan Deng
 */
public class IndicatorFunction {
    
    public static float dmPlus(int idx, List<Float> highs, List<Float> lows) {
        float value = Float.NaN;
        
        if (idx == 0) {
            
            value = Float.NaN;
            
        } else {
            
            if (highs.get(idx) > highs.get(idx - 1) && lows.get(idx) > lows.get(idx - 1)) {
                value = highs.get(idx) - highs.get(idx - 1);
            } else if (highs.get(idx) < highs.get(idx - 1) && lows.get(idx) < lows.get(idx - 1)) {
                value = 0f;
            } else if (highs.get(idx) > highs.get(idx - 1) && lows.get(idx) < lows.get(idx - 1)) {
                if (highs.get(idx) - highs.get(idx - 1) > lows.get(idx - 1) - lows.get(idx)) {
                    value = highs.get(idx) - highs.get(idx - 1);
                } else {
                    value = 0f;
                }
            } else if (highs.get(idx) < highs.get(idx - 1) && lows.get(idx) > lows.get(idx - 1)) {
                value = 0f;
            } else if (highs.get(idx) == highs.get(idx - 1) && lows.get(idx) == lows.get(idx - 1)) {
                value = 0f;
            } else if (lows.get(idx) > highs.get(idx - 1)) {
                value = highs.get(idx) - highs.get(idx);
            } else if (highs.get(idx) < lows.get(idx - 1)) {
                value = 0f;
            } else {
                value = 0f;
            }
            
        }
        
        return value;
    }
    
    public static float dmMinus(int idx, List<Float> highs, List<Float> lows) {
        float value = Float.NaN;
        
        if (idx == 0) {
            
            value = Float.NaN;
            
        } else {
            
            if (highs.get(idx) > highs.get(idx - 1) && lows.get(idx) > lows.get(idx - 1)) {
                value = 0f;
            } else if (highs.get(idx) < highs.get(idx - 1) && lows.get(idx) < lows.get(idx - 1)) {
                value = lows.get(idx - 1) - lows.get(idx);
            } else if (highs.get(idx) > highs.get(idx - 1) && lows.get(idx) < lows.get(idx - 1)) {
                if (highs.get(idx) - highs.get(idx - 1) > lows.get(idx - 1) - lows.get(idx)) {
                    value = 0f;
                } else {
                    value = lows.get(idx - 1) - lows.get(idx);
                }
            } else if (highs.get(idx) < highs.get(idx - 1) && lows.get(idx) > lows.get(idx - 1)) {
                value = 0f;
            } else if (highs.get(idx) == highs.get(idx - 1) && lows.get(idx) == lows.get(idx - 1)) {
                value = 0f;
            } else if (lows.get(idx) > highs.get(idx - 1)) {
                value = 0f;
            } else if (highs.get(idx) < lows.get(idx - 1)) {
                value = lows.get(idx - 1) - lows.get(idx);
            } else {
                value = 0f;
            }
            
        }
        
        return value;
    }
    
    public static float tr(int idx, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx == 0) {
            
            value = Float.NaN;
            
        } else {
            
            float tr_tmp = Math.max(highs.get(idx) - lows.get(idx), Math.abs(highs.get(idx) - closes.get(idx - 1)));
            value = Math.max(tr_tmp, Math.abs(lows.get(idx) - closes.get(idx - 1)));
            
        }
        
        return value;
    }
    
    public static float diPlus(int idx, int period, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx < period - 1) {
            
            value = Float.NaN;
            
        } else {
            
            List<Float> dms = new ArrayList<Float>(period);
            List<Float> trs = new ArrayList<Float>(period);
            
            int fromIdx = idx - (period - 1);
            int toIdx   = idx;
            
            for (int i = fromIdx; i <= toIdx; i++) {
                
                dms.add(dmPlus(i, highs, lows));
                trs.add(tr(i, highs, lows, closes));
                
            }
            
            float ma_dm = StatisticFunction.ma(dms, 0, period - 1);
            float ma_tr = StatisticFunction.ma(trs, 0, period - 1);
            
            value = (ma_tr == 0) ?
                0 : ma_dm / ma_tr * 100f;
            
        }
        
        return value;
    }
    
    public static float diMinus(int idx, int period, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx < period - 1) {
            
            value = Float.NaN;
            
        } else {
            
            List<Float> dms = new ArrayList<Float>(period);
            List<Float> trs = new ArrayList<Float>(period);
            
            int fromIdx = idx - (period - 1);
            int toIdx   = idx;
            
            for (int i = fromIdx; i <= toIdx; i++) {
                
                dms.add(dmMinus(i, highs, lows));
                trs.add(tr(i, highs, lows, closes));
                
            }
            
            float ma_dm = StatisticFunction.ma(dms, 0, period - 1);
            float ma_tr = StatisticFunction.ma(trs, 0, period - 1);
            
            value = (ma_tr == 0) ?
                0 : ma_dm / ma_tr * 100f;
            
        }
        
        return value;
    }
    
    public static float dx(int idx, int period, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx < period - 1) {
            
            value = Float.NaN;
            
        } else {
            
            float diPlus  = diPlus( idx, period, highs, lows, closes);
            float diMinus = diMinus(idx, period, highs, lows, closes);
            
            value = (diPlus + diMinus == 0) ?
                0 : Math.abs(diPlus - diMinus) / (diPlus + diMinus) * 100f;
            
        }
        
        return value;
    }
    
    public static float adx(int idx, int periodDI, int periodADX, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx < periodDI - 1 || idx < periodADX - 1) {
            
            value = Float.NaN;
            
        } else {
            
            List<Float> dxes = new ArrayList<Float>(periodADX);
            
            int fromIdx = idx - (periodADX - 1);
            int toIdx   = idx;
            
            for (int i = fromIdx; i <= toIdx; i++) {
                
                dxes.add(dx(i, periodDI, highs, lows, closes));
                
            }
            
            value = StatisticFunction.ma(dxes, 0, periodADX - 1);
            
        }
        
        return value;
    }
    
    public static float adxr(int idx, int periodDI, int periodADX, List<Float> highs, List<Float> lows, List<Float> closes) {
        float value = Float.NaN;
        
        if (idx < periodDI - 1 || idx < periodADX - 1) {
            
            value = Float.NaN;
            
        } else {
            
            float adx1 = adx(idx,             periodDI, periodADX, highs, lows, closes);
            float adx2 = adx(idx - periodADX, periodDI, periodADX, highs, lows, closes);
            
            value = (adx1 + adx2) / 2f;
            
        }
        
        return value;
    }
    
}


