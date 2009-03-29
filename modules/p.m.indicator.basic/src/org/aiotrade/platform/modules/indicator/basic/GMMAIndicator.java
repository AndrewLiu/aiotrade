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

import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.platform.core.analysis.indicator.AbstractContIndicator;
import org.aiotrade.platform.core.analysis.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.platform.core.analysis.indicator.IndicatorName;

/**
 * Guppy multiple Moving Average
 *
 * @author Caoyuan Deng
 */
@IndicatorName("GMMA")
public class GMMAIndicator extends AbstractContIndicator {
    
    Opt period01 = new DefaultOpt("Period Short 1", 3.0);
    Opt period02 = new DefaultOpt("Period Short 2", 5.0);
    Opt period03 = new DefaultOpt("Period Short 3", 8.0);
    Opt period04 = new DefaultOpt("Period Short 4", 10.0);
    Opt period05 = new DefaultOpt("Period Short 5", 12.0);
    Opt period06 = new DefaultOpt("Period Short 6", 15.0);
    Opt period07 = new DefaultOpt("Period Long 1",  30.0);
    Opt period08 = new DefaultOpt("Period Long 2",  35.0);
    Opt period09 = new DefaultOpt("Period Long 3",  40.0);
    Opt period10 = new DefaultOpt("Period Long 4",  45.0);
    Opt period11 = new DefaultOpt("Period Long 5",  50.0);
    Opt period12 = new DefaultOpt("Period Long 6",  60.0);
    
    Var<Float> ma01 = new DefaultVar("MA01", Plot.Line);
    Var<Float> ma02 = new DefaultVar("MA02", Plot.Line);
    Var<Float> ma03 = new DefaultVar("MA03", Plot.Line);
    Var<Float> ma04 = new DefaultVar("MA04", Plot.Line);
    Var<Float> ma05 = new DefaultVar("MA05", Plot.Line);
    Var<Float> ma06 = new DefaultVar("MA06", Plot.Line);
    Var<Float> ma07 = new DefaultVar("MA07", Plot.Line);
    Var<Float> ma08 = new DefaultVar("MA08", Plot.Line);
    Var<Float> ma09 = new DefaultVar("MA09", Plot.Line);
    Var<Float> ma10 = new DefaultVar("MA10", Plot.Line);
    Var<Float> ma11 = new DefaultVar("MA11", Plot.Line);
    Var<Float> ma12 = new DefaultVar("MA12", Plot.Line);
    
    {
        _sname = "GMMA";
        _lname = "Guppy Multiple Moving Average";
    }

    protected void computeCont(int begIdx) {
        for (int i = begIdx; i < _itemSize; i++) {
            ma01.set(i, ma(i, C, period01));
            ma02.set(i, ma(i, C, period02));
            ma03.set(i, ma(i, C, period03));
            ma04.set(i, ma(i, C, period04));
            ma05.set(i, ma(i, C, period05));
            ma06.set(i, ma(i, C, period06));
            ma07.set(i, ma(i, C, period07));
            ma08.set(i, ma(i, C, period08));
            ma09.set(i, ma(i, C, period09));
            ma10.set(i, ma(i, C, period10));
            ma11.set(i, ma(i, C, period11));
            ma12.set(i, ma(i, C, period12));
        }
    }
    
}




