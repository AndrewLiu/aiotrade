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

import org.aiotrade.platform.core.analysis.indicator.Direction;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class SARFunction extends AbstractFunction {
    
    Opt initial, step, maximum;
    
    Var<Direction> direction = new DefaultVar();
    Var<Float>     ep        = new DefaultVar();
    Var<Float>     af        = new DefaultVar();
    
    Var<Float> sar = new DefaultVar();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.initial = (Opt)args[0];
        this.step = (Opt)args[1];
        this.maximum = (Opt)args[2];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.initial == args[0] &&
                this.step == args[1] &&
                this.maximum == args[2];
    }

    protected void computeSpot(int i) {
        if (i == 0) {
            
            direction.set(i, Direction.Long);
            
            float currLow = L.get(i);
            sar.set(i, currLow);
            
            af.set(i, initial.value());
            
            float currHigh = H.get(i);
            ep.set(i, currHigh);
            
        } else {
            
            if (direction.get(i - 1) == Direction.Long) {
                /** in long-term */
                
                float currHigh = H.get(i);
                float prevHigh = H.get(i - 1);
                
                if (currHigh > ep.get(i - 1)) {
                    /** new high, acceleration adds 'step' each day, till 'maximum' */
                    af.set(i, Math.min(af.get(i - 1) + step.value(), maximum.value()));
                    ep.set(i, currHigh);
                } else {
                    /** keep same acceleration */
                    af.set(i, af.get(i - 1));
                    ep.set(i, ep.get(i - 1));
                }
                sar.set(i, sar.get(i - 1) + af.get(i) * (prevHigh - sar.get(i - 1)));
                
                if (sar.get(i) >= currHigh) {
                    /** turn to short-term */
                    
                    direction.set(i, Direction.Short);
                    
                    sar.set(i, currHigh);
                    
                    af.set(i, initial.value());
                    ep.set(i, L.get(i));
                    
                } else {
                    /** still in long-term */
                    
                    direction.set(i, Direction.Long);
                }
                
            } else {
                /** in short-term */
                
                float currLow = L.get(i);
                float prevLow = L.get(i - 1);
                
                if (currLow < ep.get(i - 1)) {
                    af.set(i, Math.min(af.get(i - 1) + step.value(), maximum.value()));
                    ep.set(i, currLow);
                } else {
                    af.set(i, af.get(i - 1));
                    ep.set(i, ep.get(i - 1));
                }
                sar.set(i, sar.get(i - 1) + af.get(i) * (prevLow - sar.get(i - 1)));
                
                if (sar.get(i) <= currLow) {
                    /** turn to long-term */
                    
                    direction.set(i, Direction.Long);
                    
                    sar.set(i, currLow);
                    
                    af.set(i, initial.value());
                    ep.set(i, H.get(i));
                    
                } else {
                    /** still in short-term */
                    
                    direction.set(i, Direction.Short);
                }
            }
            
        }
    }
    
    public final float getSar(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return sar.get(idx);
    }
    
    public final Direction getSarDirection(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return direction.get(idx);
    }
}




