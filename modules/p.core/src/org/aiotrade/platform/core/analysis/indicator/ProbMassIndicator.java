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

import org.aiotrade.platform.core.analysis.indicator.AbstractIndicator.DefaultOpt;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.DefaultSer.DefaultVar;
import org.aiotrade.lib.math.timeseries.SerItem;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;

/**
 *
 * @author Caoyuan Deng
 */
@IndicatorName("ProbMass")
public class ProbMassIndicator extends AbstractSpotIndicator {
    {
        _sname = "Probability Mass";
        _lname = "Probability Mass";
        _overlapping = true;
    }
    
    
    Var appledVar;
    
    public ProbMassIndicator(Ser baseSeries) {
        super(baseSeries);
    }
    
    public void setAppliedVar(Var var) {
        this.appledVar = var;
    }
    
    public String getShortDescription() {
        if (appledVar != null) {
            return "PM: " + appledVar.getName();
        } else {
            return "PM";
        }
    }
    
    Opt nIntervals = new DefaultOpt("Number of Intervals", 30.0, 1.0, 1.0, 100.0);
    Opt period1    = new DefaultOpt("Period1",  50.0);
    Opt period2    = new DefaultOpt("Period2",  100.0);
    Opt period3    = new DefaultOpt("Period3",  200.0);
    
    
    DefaultVar<Float[][]> MASS1 = new DefaultVar("MASS1", Plot.Profile);
    DefaultVar<Float[][]> MASS2 = new DefaultVar("MASS2", Plot.Profile);
    DefaultVar<Float[][]> MASS3 = new DefaultVar("MASS3", Plot.Profile);
    
    public SerItem computeSpot(long time, int masterIdx) {
        SerItem item = createItemOrClearIt(time);
        
        Float[][] probability_mass1 = probMass(masterIdx, appledVar, period1, nIntervals);
        Float[][] probability_mass2 = probMass(masterIdx, appledVar, period2, nIntervals);
        Float[][] probability_mass3 = probMass(masterIdx, appledVar, period3, nIntervals);
        
        item.set(MASS1, probability_mass1);
        item.set(MASS2, probability_mass2);
        item.set(MASS3, probability_mass3);
        
        return item;
    }
}








