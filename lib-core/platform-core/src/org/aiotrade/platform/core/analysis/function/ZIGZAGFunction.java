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
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
public class ZIGZAGFunction extends AbstractFunction {
    
    Opt percent;
    
    Var<Float>     peakHi    = new DefaultVar<Float>();
    Var<Float>     peakLo    = new DefaultVar<Float>();
    Var<Integer>   peakHiIdx = new DefaultVar<Integer>();
    Var<Integer>   peakLoIdx = new DefaultVar<Integer>();
    Var<Direction> direction = new DefaultVar<Direction>();
    
    Var<Float> zigzag        = new DefaultVar<Float>();
    Var<Float> pseudoZigzag  = new DefaultVar<Float>();
    
    public void set(Ser baseSer, Object... args) {
        super.set(baseSer);
        
        this.percent = (Opt)args[0];
    }
    
    public boolean idEquals(Ser baseSer, Object... args) {
        return this._baseSer == baseSer &&
                
                this.percent == args[0];
    }

    /** 
     * @TODO
     * Re-think how to effictively get this pseudoZigzag
     */
    protected void preComputeTo(final long sessionId, final int idx) {
        /**
         * the last zigzag is not a real turn over point, it's just a peakLo/Hi
         * in last trend, so should clear it. and if necessary, re compute
         * from this point.
         */
//        int lastPeakIdx = indexOfLastValidValue(pseudoZigzag);
//        if (lastPeakIdx >= 0) {
//            pseudoZigzag.set(lastPeakIdx, Float.NaN);
//            
//            setComputedIdx(Math.min(getComputedIdx(), lastPeakIdx));
//        }
    }
    
    protected void postComputeTo(final long sessionId, final int idx) {

        int lastIdx = _itemSize - 1;

        /** 
         * did this computing session compute till the last item? if not, do not
         * try to compute pseudo zigzag (ie. last peakHi/Lo in current trend)
         */
        if (idx != lastIdx) {
            return;
        }
        
        /** get the last zigzag as the first pseudo point */
        int lastZigzagIdx = indexOfLastValidValue(zigzag);
        if (lastZigzagIdx >= 0) {
            pseudoZigzag.set(lastZigzagIdx, zigzag.get(lastZigzagIdx));
        }
        
        /** set pseudo zigzag to the last peakHi/Lo in current trend */
        if (lastIdx >= 0) {
            if (direction.get(lastIdx) == Direction.Long) {
                int lastPeakHiIdx = peakHiIdx.get(lastIdx);
                pseudoZigzag.set(lastPeakHiIdx, H.get(lastPeakHiIdx));
            } else {
                int lastPeakLoIdx = peakLoIdx.get(lastIdx);
                pseudoZigzag.set(lastPeakLoIdx, L.get(lastPeakLoIdx));
            }
        }
        
    }
    
    protected void computeSpot(int i) {
        
        if (i == 0) {
            
            direction.set(i, Direction.Long);
            zigzag.set(i, Float.NaN);
            pseudoZigzag.set(i, Float.NaN);
            peakHi.set(i, H.get(i));
            peakLo.set(i, L.get(i));
            peakHiIdx.set(i, i);
            peakLoIdx.set(i, i);
            
        } else {
            
            if (direction.get(i - 1) == Direction.Long) {
                
                if ((H.get(i) - peakHi.get(i - 1)) / peakHi.get(i - 1) <= -percent.value()) {
                    /** turn over to short trend */
                    direction.set(i, Direction.Short);
                    
                    /** and we get a new zigzag peak of high at (idx - 1) */
                    int newZigzagIdx = peakHiIdx.get(i - 1);
                    zigzag.set(newZigzagIdx, H.get(newZigzagIdx));
                    
                    peakLo.set(i, L.get(i));
                    peakLoIdx.set(i, i);
                    
                } else {
                    /** long trend goes on */
                    direction.set(i, direction.get(i - 1));
                    
                    if (H.get(i) > peakHi.get(i - 1)) {
                        /** new high */
                        peakHi.set(i, H.get(i));
                        peakHiIdx.set(i, i);
                    } else {
                        /** keep same */
                        peakHi.set(i, peakHi.get(i - 1));
                        peakHiIdx.set(i, peakHiIdx.get(i - 1));
                    }
                    
                }
                
            } else {
                
                if ((L.get(i) - peakLo.get(i - 1)) / peakLo.get(i - 1) >= percent.value()) {
                    /** turn over to long trend */
                    direction.set(i, Direction.Long);
                    
                    /** and we get a new zigzag peak of low at (idx - 1) */
                    int newZigzagIdx = peakLoIdx.get(i - 1);
                    zigzag.set(newZigzagIdx, L.get(newZigzagIdx));
                    
                    peakHi.set(i, H.get(i));
                    peakHiIdx.set(i, i);
                    
                } else {
                    /** short trend goes on */
                    direction.set(i, direction.get(i - 1));
                    
                    if (L.get(i) < peakLo.get(i - 1)) {
                        /** new low */
                        peakLo.set(i, L.get(i));
                        peakLoIdx.set(i, i);
                    } else {
                        /** keep same */
                        peakLo.set(i, peakLo.get(i - 1));
                        peakLoIdx.set(i, peakLoIdx.get(i - 1));
                    }
                    
                }
                
            }
        }
        
    }
    
    public final float getZigzag(long sessionId, int idx) {
        /**
         * @NOTICE
         * as zigzag's value is decided by future (+n step) idx, we should 
         * go on computing untill a turn over happened.
         */
        for (int i = idx, n = _baseSer.itemList().size(); i < n; i++) {
            computeTo(sessionId, i);
            if (i > 0 && direction.get(i - 1) != direction.get(i)) {
                /** a turn over happened */
                break;
            } 
        }
        
        return zigzag.get(idx);
    }
    
    public final float getPseudoZigzag(long sessionId, int idx) {
        /** 
         * @NOTICE
         * as pseudo zigzag's value is decided by future (+n step) idx, we should 
         * go on computing untill a turn over happened.
         */
        for (int i = idx, n = _baseSer.itemList().size(); i < n; i++) {
            computeTo(sessionId, i);
            if (i > 0 && direction.get(i - 1) != direction.get(i)) {
                /** a turn over happened */
                break;
            } 
        }
        
        return pseudoZigzag.get(idx);
    }

    public final Direction getZigzagDirection(long sessionId, int idx) {
        /** 
         * @NOTICE
         * as zigzag direction 's value is decided by future (+n step) idx, we should 
         * go on computing untill a turn over happened.
         */
        for (int i = idx, n = _baseSer.itemList().size(); i < n; i++) {
            computeTo(sessionId, i);
            if (i > 0 && direction.get(i - 1) != direction.get(i)) {
                /** a turn over happened */
                break;
            } 
        }
        
        return direction.get(idx);
    }
}





