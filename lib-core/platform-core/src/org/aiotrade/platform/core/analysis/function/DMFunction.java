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

import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class DMFunction extends AbstractFunction {
    
    Var<Float> dmPlus  = new DefaultVar();
    Var<Float> dmMinus = new DefaultVar();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer;
    }
    
    protected void computeSpot(int i) {
        if (i == 0) {
            
            dmPlus .set(i, Float.NaN);
            dmMinus.set(i, Float.NaN);
            
        } else {
            
            if (H.get(i) > H.get(i - 1) && L.get(i) > L.get(i - 1)) {
                dmPlus .set(i, H.get(i) - H.get(i - 1));
                dmMinus.set(i, 0f);
            } else if (H.get(i) < H.get(i - 1) && L.get(i) < L.get(i - 1)) {
                dmPlus .set(i, 0f);
                dmMinus.set(i, L.get(i - 1) - L.get(i));
            } else if (H.get(i) > H.get(i - 1) && L.get(i) < L.get(i - 1)) {
                if (H.get(i) - H.get(i - 1) > L.get(i - 1) - L.get(i)) {
                    dmPlus .set(i, H.get(i) - H.get(i - 1));
                    dmMinus.set(i, 0f);
                } else {
                    dmPlus .set(i, 0f);
                    dmMinus.set(i, L.get(i - 1) - L.get(i));
                }
            } else if (H.get(i) < H.get(i - 1) && L.get(i) > L.get(i - 1)) {
                dmPlus .set(i, 0f);
                dmMinus.set(i, 0f);
            } else if (H.get(i) == H.get(i - 1) && L.get(i) == L.get(i - 1)) {
                dmPlus .set(i, 0f);
                dmMinus.set(i, 0f);
            } else if (L.get(i) > H.get(i - 1)) {
                dmPlus .set(i, H.get(i) - H.get(i));
                dmMinus.set(i, 0f);
            } else if (H.get(i) < L.get(i - 1)) {
                dmPlus .set(i, 0f);
                dmMinus.set(i, L.get(i - 1) - L.get(i));
            } else {
                dmPlus .set(i, 0f);
                dmMinus.set(i, 0f);
            }
            
        }
    }
    
    public final float getDmPlus(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return dmPlus.get(idx);
    }
    
    public final float getDmMinus(long sessionId, int idx) {
        computeTo(sessionId, idx);
        
        return dmMinus.get(idx);
    }
}

