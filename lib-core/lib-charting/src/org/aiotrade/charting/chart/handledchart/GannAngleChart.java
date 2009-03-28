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
import org.aiotrade.charting.util.GeomUtil;
import org.aiotrade.charting.widget.PathWidget;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.charting.chart.AbstractChart;
import org.aiotrade.charting.chart.handledchart.GannAngleChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.Label;

/**
 *
 * @author Caoyuan Deng
 */
public class GannAngleChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long  t1;
        float v1;
        long  t2;
        float v2;
        
        public void set(long t1, float v1, long t2, float v2) {
            this.t1 = t1;
            this.v1 = v1;
            this.t2 = t2;
            this.v2 = v2;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        
        float[] xs = new float[2];
        float[] ys = new float[2];
        
        int b1 = bt(model.t1);
        int b2 = bt(model.t2);
        
        float dBar = b2 - b1;
        dBar = dBar == 0 ? 1 : dBar;
        
        float rate = (model.v2 - model.v1) / dBar;
        
        xs[0] = xb(bt(model.t1));
        xs[1] = xb(bt(model.t2));
        ys[0] = yv(model.v1);
        ys[1] = yv(model.v2);
        
        Label label1 = addChild(new Label());
        label1.setFont(LookFeel.getCurrent().axisFont);
        label1.setForeground(color);
        label1.model().set(xs[1] + 2, ys[1], Float.toString(rate));
        label1.plot();
        
        final PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        final GeneralPath path = pathWidget.getPath();
        
        plotOneDirection(xs, ys, true, true, true, path);
        
        xs[0] = xb(bt(model.t2));
        xs[1] = xb(bt(model.t1));
        ys[0] = yv(model.v2);
        ys[1] = yv(model.v1);
        plotOneDirection(xs, ys, false, false, false, path);
        
        xs[0] = xb(bt(model.t1));
        xs[1] = xb(bt(model.t2));
        ys[0] = yv(model.v2);
        ys[1] = yv(model.v1);
        plotOneDirection(xs, ys, true, false, false, path);
        
        xs[0] = xb(bt(model.t2));
        xs[1] = xb(bt(model.t1));
        ys[0] = yv(model.v1);
        ys[1] = yv(model.v2);
        plotOneDirection(xs, ys, false, false, false, path);
    }
    
    /**
     * should avoid dupliacte line
     */
    private void plotOneDirection(float[] xs, float[] ys, boolean drawMain, boolean drawHorizontal, boolean drawVertical, final GeneralPath path) {
        final Model model = model();
        
        float k = xs[1] - xs[0] == 0 ? 1 : (ys[1] - ys[0]) / (xs[1] - xs[0]);
        float xmin = Math.min(xs[0], xs[1]);
        float xmax = Math.max(xs[0], xs[1]);
        float ymin = Math.min(ys[0], ys[1]);
        float ymax = Math.max(ys[0], ys[1]);
        
        /** main angle */
        if (drawMain) {
            plotLineSegment(xs[0], ys[0], xs[1], ys[1], path);
        }
        
        /** horizontal angle */
        if (drawHorizontal) {
            plotLineSegment(xs[0], ys[0], xs[1], ys[0], path);
            plotLineSegment(xs[0], ys[1], xs[1], ys[1], path);
        }
        
        /** vertical angle */
        if (drawVertical) {
            plotVerticalLineSegment(bt(model.t1), ymin, ymax, path);
            plotVerticalLineSegment(bt(model.t2), ymin, ymax, path);
        }
        
        float xlast = xb(0);
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            float x1 = xlast;
            float x2 = xb(bar);
            if (x1 >= xmin && x1 <= xmax && x2 >= xmin && x2 <= xmax) {
                float y1;
                float y2;
                for (float j = 2; j <= 3; j++) {
                    y1 = GeomUtil.yOfLine(x1, xs[0], ys[0], k * j);
                    y2 = GeomUtil.yOfLine(x2, xs[0], ys[0], k * j);
                    if (y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax) {
                        path.moveTo(x1, y1);
                        path.lineTo(x2, y2);
                    }
                    
                    y1 = GeomUtil.yOfLine(x1, xs[0], ys[0], k / j);
                    y2 = GeomUtil.yOfLine(x2, xs[0], ys[0], k / j);
                    if (y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax) {
                        path.moveTo(x1, y1);
                        path.lineTo(x2, y2);
                    }
                }
            }
            
            xlast = x2;
            
        }
        
    }
    
}
