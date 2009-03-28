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
package org.aiotrade.charting.chart;

import java.util.Set;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.math.util.Signal;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 1, 2006, 3:04 AM
 * @since   1.0.4
 */
public class ChartFactory {
    
    public static Chart<?> createVarChart(Set<Var<?>> chartVarContainer, Var<?> var) {
        Chart<?> chart = null;
        switch (var.getPlot()) {
            case Volume:
                chart = new VolumeChart();
                ((VolumeChart)chart).model().set(false);
                chartVarContainer.add(var);
                break;
            case Line:
                chart = new PolyLineChart();
                ((PolyLineChart)chart).model().set(var);
                chartVarContainer.add(var);
                break;
            case Stick:
                chart = new StickChart();
                ((StickChart)chart).model().set(var);
                chartVarContainer.add(var);
                break;
            case Dot:
                chart = new DotChart();
                ((DotChart)chart).model().set(var);
                chartVarContainer.add(var);
                break;
            case Shade:
                chart = new GradientChart();
                ((GradientChart)chart).model().set(var, null);
                chartVarContainer.add(var);
                break;
            case Profile:
                chart = new ProfileChart();
                ((ProfileChart)chart).model().set(var);
                chartVarContainer.add(var);
                break;
            case Zigzag:
                chart = new ZigzagChart();
                ((ZigzagChart)chart).model().set(var);
                chartVarContainer.add(var);
                break;
            case Signal:
                chart = new SignalChart();
                ((SignalChart)chart).model().set((Var<Signal>)var);
                chartVarContainer.add(var);
                break;
                
            default:
        }
        
        return chart;
    }
    
    
}
