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

import org.aiotrade.lib.math.StatisticFunction;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class PROBMASSFunction extends AbstractFunction {
    
    Opt period, nInterval;
    Var<Float> var, weight;
    
    /**
     * as this function do not remember previous valus, do not need a Var as probMass
     */
    Float[][] probMass;
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.var = (Var<Float>)args[0];
        this.weight = (Var<Float>)args[1];
        this.period = (Opt)args[2];
        this.nInterval = (Opt)args[3];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.var == args[0] &&
                (args[1] == null && this.weight == null || args[1] != null && this.weight == args[1]) &&
                this.period == args[2] &&
                this.nInterval == args[3];
    }

    protected void computeSpot(int i) {
        if (weight == null) {
            
            probMass = probMass(i, var, period.value(), nInterval.value());
            
        } else {
            
            probMass = probMass(i, var, weight, period.value(), nInterval.value());
            
        }
    }
    
    private final static Float[][] probMass(int idx, Var<Float> var, Float period, Float nInterval) {
        int begIdx = idx - period.intValue() + 1;
        int endIdx = idx;
        
        return StatisticFunction.probMass(var.values(), begIdx, endIdx, nInterval.intValue());
    }
    
    private final static Float[][] probMass(int idx, Var<Float> var, Var<Float> weight, Float period, Float nInterval) {
        int begIdx = idx - period.intValue() + 1;
        int endIdx = idx;
        
        return StatisticFunction.probMass(var.values(), weight.values(), begIdx, endIdx, nInterval.intValue());
    }
    
    /**
     * override compute(int), this function is not dependent on previous values
     */
    public void compute(final int idx) {
        computeSpot(idx);
    }
    
    public final Float[][] getProbMass(long sessionId, int idx) {
        compute(idx);
        
        return probMass;
    }
    
}




