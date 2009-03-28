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
import org.aiotrade.charting.chart.handledchart.ParallelLinesChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class ParallelLinesChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long  t1;
        float v1;
        long  t2;
        float v2;
        long  t3;
        float v3;
        
        public void set(long t1, float v1, long t2, float v2, long t3, float v3) {
            this.t1 = t1;
            this.v1 = v1;
            this.t2 = t2;
            this.v2 = v2;
            this.t3 = t3;
            this.v3 = v3;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        
        float[] xs = new float[3];
        float[] ys = new float[3];
        xs[0] = xb(bt(model.t1));
        xs[1] = xb(bt(model.t2));
        xs[2] = xb(bt(model.t3));
        ys[0] = yv(model.v1);
        ys[1] = yv(model.v2);
        ys[2] = yv(model.v3);
        
        float dx = xs[1] - xs[0];
        float dy = ys[1] - ys[0];
        
        float k = (dx == 0) ? 1 : dy / dx;
        
        double distance = Math.abs(k * xs[2] - ys[2] + ys[0] - k * xs[0]) / Math.sqrt(k * k + 1);
        
        final PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        final GeneralPath path = pathWidget.getPath();
        
        plotLine(xs[0], ys[0], k, path);
        
        if (distance >= 1) {
            plotLine(xs[2], ys[2], k, path);
        }
        
    }
    
}


