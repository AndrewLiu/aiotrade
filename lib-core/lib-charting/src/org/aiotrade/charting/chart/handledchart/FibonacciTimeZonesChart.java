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
package org.aiotrade.charting.chart.handledchart;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.charting.chart.AbstractChart;
import org.aiotrade.charting.chart.handledchart.FibonacciTimeZonesChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class FibonacciTimeZonesChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long t1;
        
        public void set(long t1) {
            this.t1 = t1;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        
        int numFn = 40;
        
        float[] bs = new float[numFn * 2 + 1];
        bs[0] = bt(model.t1);
        
        /** calculate Fibonacci serials */
        float Fn[] = new float[numFn];
        Fn[0] = 5;
        Fn[1] = 8;
        
        /**
         * @NOTICE
         * only apply compressedFactor to distance between bar index, not to x.
         */
        bs[1] = bs[0] + Fn[0];
        bs[2] = bs[0] - Fn[0];
        bs[3] = bs[0] + Fn[1];
        bs[4] = bs[0] - Fn[1];
        for (int n = 2; n < numFn; n++) {
            
            /*- @RESERVE
             * double Fn = 100 * Math.pow(Math.PI, 2) * Math.pow(0.6180339, 15 - i);
             * int step = (int)(Math.sqrt(Fn) * 29.5306);
             */
            
            Fn[n] = Fn[n - 1] + Fn[n - 2];
            
            /** positive side */
            bs[n * 2 + 1] = bs[0] + Fn[n];
            /** negative side */
            bs[n * 2 + 2] = bs[0] - Fn[n];
        }
        
        PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        GeneralPath path = pathWidget.getPath();
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            for (int i = 0; i < nBarsCompressed; i++) {
                if (bar + i == Math.round(bs[0])) {
                    plotVerticalLine(bar + i, path);
                }
                
                /** search if i is in Fibonacci serials */
                for (int j = 1; j < numFn * 2; j += 2) {
                    if (bar + i == Math.round(bs[j]) || bar + i== Math.round(bs[j + 1])) {
                        plotVerticalLine(bar + i, path);
                        break;
                    }
                }
            }
        }
        
    }
    
}

