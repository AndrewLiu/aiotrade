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

import org.aiotrade.platform.core.analysis.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.platform.core.analysis.indicator.AbstractSpotIndicator;
import org.aiotrade.platform.core.analysis.indicator.IndicatorName;
import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("HVD")
public class HVDIndicator extends AbstractSpotIndicator {
    
    Opt nIntervals = new DefaultOpt("Number of Intervals", 30.0, 1.0, 1.0, 100.0);
    Opt period1    = new DefaultOpt("Period1",  50.0);
    Opt period2    = new DefaultOpt("Period2",  100.0);
    Opt period3    = new DefaultOpt("Period3",  200.0);
    
    Var<Float[][]> HVD1 = new DefaultVar("HVD1", Plot.Profile);
    Var<Float[][]> HVD2 = new DefaultVar("HVD2", Plot.Profile);
    Var<Float[][]> HVD3 = new DefaultVar("HVD3", Plot.Profile);
    
    {
        _sname = "HVD";
        _lname = "Historical Volume Distribution";
        _overlapping = true;
    }

    public SerItem computeSpot(long time, int baseIdx) {
        SerItem item = createItemOrClearIt(time);
        
        Float[][] probability_mass1 = probMass(baseIdx, C, V, period1, nIntervals);
        Float[][] probability_mass2 = probMass(baseIdx, C, V, period2, nIntervals);
        Float[][] probability_mass3 = probMass(baseIdx, C, V, period3, nIntervals);
        
        item.set(HVD1, probability_mass1);
        item.set(HVD2, probability_mass2);
        item.set(HVD3, probability_mass3);
        
        return item;
    }
}







