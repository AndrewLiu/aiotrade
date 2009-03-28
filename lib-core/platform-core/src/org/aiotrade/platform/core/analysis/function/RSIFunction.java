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
package org.aiotrade.platform.core.analysis.function;

import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class RSIFunction extends AbstractFunction {
    
    Opt period;
    
    Var<Float> up = new DefaultVar();
    Var<Float> dn = new DefaultVar();
    
    Var<Float> rsi = new DefaultVar();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.period = (Opt)args[0];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.period == args[0];
    }

    protected void computeSpot(int i) {
        if (i == 0) {
            
            up.set(i, Float.NaN);
            dn.set(i, Float.NaN);
            
            rsi.set(i, Float.NaN);
            
        } else {
            
            float change = C.get(i) - C.get(i - 1);
            if (change > 0) {
                up.set(i, change);
                dn.set(i, 0f);
            } else {
                up.set(i, 0f);
                dn.set(i, -change);
            }
            
            if (i < period.value() - 1) {
                
                rsi.set(i, Float.NaN);
            
            } else {
            
                float up_sum_i = sum(i, up, period);
                float dn_sum_i = sum(i, dn, period);
                
                rsi.set(i, (up_sum_i + dn_sum_i == 0) ? 0 : up_sum_i / (up_sum_i + dn_sum_i) * 100f);
            }
        }
    }
    
    public final float getRsi(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return rsi.get(idx);
    }
    
}


