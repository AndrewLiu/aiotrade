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
import org.aiotrade.charting.chart.handledchart.FibonacciVerticalRetracementsChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.Label;
import org.aiotrade.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class FibonacciVerticalRetracementsChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long t1;
        long t2;
        
        public void set(long t1, long t2) {
            this.t1 = t1;
            this.t2 = t2;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        
        float[] bs = new float[2];
        bs[0] = bt(model.t1);
        bs[1] = bt(model.t2);
        float interval = bs[1] - bs[0];
        
        /** calculate Fibonacci serials */
        float Fn[] = {
            0.000f,
            0.382f,
            0.500f,
            0.618f,
            1.000f,
            1.236f,
            1.618f,
            2.000f,
            2.618f,
            3.000f
        };
        
        
        float ymin = yv(datumPlane.getMinValue());
        for (int n = 0; n < Fn.length; n++) {
            int b = Math.round(bs[0] + interval * Fn[n]);
            if (b >= 1 && b <= nBars) {
                PathWidget pathWidget = addChild(new PathWidget());
                pathWidget.setForeground(color);
                GeneralPath path = pathWidget.getPath();
                plotVerticalLine(b, path);
                
                final float x = xb(b);
                Label label1 = addChild(new Label());
                label1.setFont(LookFeel.getCurrent().axisFont);
                label1.setForeground(color);
                label1.model().set(x + 1, ymin, Float.toString(Fn[n]));
                label1.plot();
            }
        }
    }
    
}


