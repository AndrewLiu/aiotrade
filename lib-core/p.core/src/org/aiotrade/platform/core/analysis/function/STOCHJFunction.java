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

import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class STOCHJFunction extends AbstractFunction {
    
    Opt period, periodK, periodD;
    
    Var<Float> stochK = new DefaultVar();
    Var<Float> stochD = new DefaultVar();
    
    Var<Float> stochJ = new DefaultVar();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.period = (Opt)args[0];
        this.periodK = (Opt)args[1];
        this.periodD = (Opt)args[2];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.period == args[0] &&
                this.periodK == args[1] &&
                this.periodD == args[2];
    }

    protected void computeSpot(int i) {
        stochK.set(i, stochK(i, period, periodK));
        stochD.set(i, stochD(i, period, periodK, periodD));
        
        stochJ.set(i, stochD.get(i) + 2 * (stochD.get(i) - stochK.get(i)));
    }
    
    public final float getStochJ(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return stochJ.get(idx);
    }
    
}





