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
package org.aiotrade.platform.core.analysis.indicator;

import org.aiotrade.math.timeseries.DefaultSer.SparseVar;
import org.aiotrade.math.timeseries.computable.ContComputable;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.math.util.Sign;
import org.aiotrade.math.util.Signal;

/**
 * Abstract Signal Indicator
 *
 * @author Caoyuan Deng
 */
@IndicatorName("Abstract Signal Indicator")
public abstract class AbstractSignalIndicator extends AbstractIndicator implements ContComputable {
    
    Var<Signal> signalVar = new SparseVar<Signal>("Signal", Plot.Signal);
    
    {
        _overlapping = true;
    }
    
    public AbstractSignalIndicator() {
        super();
    }
    
    public AbstractSignalIndicator(Ser baseSer) {
        super(baseSer);
    }
    
    protected void signal(int idx, Sign sign) {
        signal(idx, sign, "");
    }
    
    protected void signal(int idx, Sign sign, String name) {
        long time = _baseSer.timestamps().get(idx);
        
        /** appoint a value for this sign as the drawing position */
        float value = Float.NaN;
        switch (sign) {
            case EnterLong:
                value = L.get(idx);
                break;
            case ExitLong:
                value = H.get(idx);
                break;
            case EnterShort:
                value = H.get(idx);
                break;
            case ExitShort:
                value = L.get(idx);
                break;
            default:
        }
        
        signalVar.set(idx, new Signal(idx, time, value, sign, name));
    }
    
    protected void removeSignal(int idx) {
        long time = _baseSer.timestamps().get(idx);
        /** @TODO */
    }
    
}

