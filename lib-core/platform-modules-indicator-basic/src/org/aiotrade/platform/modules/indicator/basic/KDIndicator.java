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
@IndicatorName("KD")
public class KDIndicator extends AbstractContIndicator {
    
    Opt period  = new DefaultOpt("Period K",           9.0);
    Opt periodK = new DefaultOpt("Period K Smoothing", 3.0);
    Opt periodD = new DefaultOpt("Period D Smoothing", 3.0);
    
    Var<Float> k = new DefaultVar("K", Plot.Line);
    Var<Float> d = new DefaultVar("D", Plot.Line);
    Var<Float> j = new DefaultVar("J", Plot.Line);
    
    {
        _sname = "KD";
        _lname = "Stochastics";
        _grids = new Float[] {20f, 80f};
    }
    
    protected void computeCont(int begIdx) {
        for (int i = begIdx; i < _itemSize; i++) {
            k.set(i, stochK(i, period, periodK));
            d.set(i, stochD(i, period, periodK, periodD));
            j.set(i, stochJ(i, period, periodK, periodD));
        }
    }
    
}


