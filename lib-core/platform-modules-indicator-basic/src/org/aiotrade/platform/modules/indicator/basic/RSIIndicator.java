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
package org.aiotrade.platform.modules.indicator.basic;

import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.platform.core.analysis.indicator.AbstractContIndicator;
import org.aiotrade.platform.core.analysis.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.platform.core.analysis.indicator.IndicatorName;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("RSI")
public class RSIIndicator extends AbstractContIndicator {
    
    Opt periodS = new DefaultOpt("Period Short",   6.0 );
    Opt periodM = new DefaultOpt("Period Mediaum", 12.0);
    Opt periodL = new DefaultOpt("Period Long",    24.0);
    
    Var<Float> rsi1 = new DefaultVar("RSI1", Plot.Line);
    Var<Float> rsi2 = new DefaultVar("RSI2", Plot.Line);
    Var<Float> rsi3 = new DefaultVar("RSI3", Plot.Line);
    
    {
        _sname = "RSI";
        _lname = "Relative Strength Index";
        _grids = new Float[] {20f, 80f};
    }
    
    protected void computeCont(int begIdx) {
        for (int i = begIdx; i < _itemSize; i++) {
            rsi1.set(i, rsi(i, periodS));
            rsi2.set(i, rsi(i, periodM));
            rsi3.set(i, rsi(i, periodL));
        }
    }
    
}


