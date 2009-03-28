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

import java.awt.Color;
import java.awt.geom.GeneralPath;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.charting.chart.DiagonalChart.Model;
import org.aiotrade.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class DiagonalChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long originTime;
        float b0;
        float step;
        float k;
        Color color;
        
        public void set(long originTime, float b0, float step, float k, Color color) {
            this.originTime = originTime;
            this.b0 = b0;
            this.step = step;
            this.k = k;
            this.color = color;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        final QuoteSer quoteSer = (QuoteSer)super.ser;
        
        Color color = model.color;
        setForeground(color);
        
        final PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        final GeneralPath path = pathWidget.getPath();
        
        final int a0 = 0; //(int)(originTime - quoteSeries.get(0).time);  // originTime's x-axis in days
        final int a1 = 0;
        final int a2 = nBars - 1;
        for (int i = 0; i < 10; i++) {
            float b1 = (a1 - a0 + i * model.step) * 365.25F / 365 * model.k + model.b0;
            float b2 = (a2 - a0 + i * model.step) * 365.25F / 365 * model.k + model.b0;
            path.moveTo(xb(a1), yv(b1));
            path.lineTo(xb(a2), yv(b2));
        }
        
    }
    
}
