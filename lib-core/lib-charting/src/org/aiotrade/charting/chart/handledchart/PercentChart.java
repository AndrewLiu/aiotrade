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
import org.aiotrade.charting.chart.handledchart.PercentChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.Label;
import org.aiotrade.charting.widget.PathWidget;


/**
 *
 * @author Caoyuan Deng
 */
public class PercentChart extends AbstractChart<Model> {
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
        xs[0] = xb(bt(model.t1));
        xs[1] = xb(bt(model.t2));
        ys[0] = yv(model.v1);
        ys[1] = yv(model.v2);
        float k = xs[1] - xs[0] == 0 ? 1 : (ys[1] - ys[0]) / (xs[1] - xs[0]);
        float interval = ys[1] - ys[0];
        float xmin = Math.min(xs[0], xs[1]);
        float xmax = Math.max(xs[0], xs[1]);
        
        float y0 = ys[0];
        float y1 = ys[0] + interval * 0.25f;
        float y2 = ys[0] + interval * 0.333333f;
        float y3 = ys[0] + interval * 0.5f;
        float y4 = ys[0] + interval * 0.666667f;
        float y5 = ys[0] + interval * 0.75f;
        float y6 = ys[1];
        
        Label label1 = addChild(new Label());
        label1.setFont(LookFeel.getCurrent().axisFont);
        label1.setForeground(color);
        label1.model().set(xs[0], y1 - 2, "25.0%");
        label1.plot();
        
        Label label2 = addChild(new Label());
        label2.setFont(LookFeel.getCurrent().axisFont);
        label2.setForeground(color);
        label2.model().set(xs[0], y2 - 2, "33.3%");
        label2.plot();
        
        Label label3 = addChild(new Label());
        label3.setFont(LookFeel.getCurrent().axisFont);
        label3.setForeground(color);
        label3.model().set(xs[0], y3 - 2, "50.0%");
        label3.plot();
        
        Label label4 = addChild(new Label());
        label4.setFont(LookFeel.getCurrent().axisFont);
        label4.setForeground(color);
        label4.model().set(xs[0], y4 - 2, "66.7%");
        label4.plot();

        Label label5 = addChild(new Label());
        label5.setFont(LookFeel.getCurrent().axisFont);
        label5.setForeground(color);
        label5.model().set(xs[0], y5 - 2, "75.0%");
        label5.plot();
        
        final PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        final GeneralPath path = pathWidget.getPath();

        float x1 = xb(0);
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            float x2 = xb(bar);
            if (x2 >= xmin && x2 <= xmax) {
                path.moveTo(x1, y0);
                path.lineTo(x2, y0);
                path.moveTo(x1, y1);
                path.lineTo(x2, y1);
                path.moveTo(x1, y2);
                path.lineTo(x2, y2);
                path.moveTo(x1, y3);
                path.lineTo(x2, y3);
                path.moveTo(x1, y4);
                path.lineTo(x2, y4);
                path.moveTo(x1, y5);
                path.lineTo(x2, y5);
                path.moveTo(x1, y6);
                path.lineTo(x2, y6);
            }
            /** should avoid the 1 point intersect at the each path's end point,
             * especially in XOR mode */
            x1 = x2 + 1;
        }
        
    }
    
}


