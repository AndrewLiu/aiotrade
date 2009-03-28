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
public class DIFunction extends AbstractFunction {
    
    Opt period;
    
    Var<Float> dmPlus  = new DefaultVar();
    Var<Float> dmMinus = new DefaultVar();
    Var<Float> tr      = new DefaultVar();
    
    Var<Float> diPlus  = new DefaultVar();
    Var<Float> diMinus = new DefaultVar();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.period = (Opt)args[0];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.period == args[0];
    }

    protected void computeSpot(int i) {
        dmPlus .set(i, dmPlus(i));
        dmMinus.set(i, dmMinus(i));
        tr     .set(i, tr(i));
        
        if (i < period.value() - 1) {
            
            diPlus .set(i, Float.NaN);
            diMinus.set(i, Float.NaN);
            
        } else {
            
            float dmPlus_ma  = ma(i, dmPlus,  period);
            float dmMinus_ma = ma(i, dmMinus, period);
            float tr_ma      = ma(i, tr,      period);
            
            float diPlus_i  = (tr_ma == 0) ? 0 : dmPlus_ma  / tr_ma * 100f;
            float diMinus_i = (tr_ma == 0) ? 0 : dmMinus_ma / tr_ma * 100f;
            
            diPlus .set(i, diPlus_i);
            diMinus.set(i, diMinus_i);
            
        }
    }
    
    public final float getDiPlus(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return diPlus.get(idx);
    }
    
    public final float getDiMinus(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return diMinus.get(idx);
    }
}


