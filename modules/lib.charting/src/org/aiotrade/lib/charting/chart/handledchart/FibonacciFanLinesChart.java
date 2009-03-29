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
package org.aiotrade.lib.charting.chart.handledchart;

import java.awt.Color;
import org.aiotrade.lib.charting.util.GeomUtil;
import org.aiotrade.lib.charting.widget.Label;
import org.aiotrade.lib.charting.widget.PathWidget;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.charting.chart.AbstractChart;
import org.aiotrade.lib.charting.chart.handledchart.FibonacciFanLinesChart.Model;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.widget.LineSegment;

/**
 *
 * @author Caoyuan Deng
 */
public class FibonacciFanLinesChart extends AbstractChart<Model> {
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
        
        LineSegment mainLine = addChild(new LineSegment());
        mainLine.setForeground(color);
        mainLine.model().set(xs[0], ys[0], xs[1], ys[1]);
        mainLine.plot();
        
        float dx = (xs[1] - xs[0]);
        float dy = ys[1] - ys[0];
        
        float k1 = dx == 0 ? 1 : dy * 0.618f / dx;
        float k2 = dx == 0 ? 1 : dy * 0.500f / dx;
        float k3 = dx == 0 ? 1 : dy * 0.382f / dx;
        
        final float xText = xs[1] + 2;
        
        Label label1 = addChild(new Label());
        label1.setFont(LookFeel.getCurrent().axisFont);
        label1.setForeground(color);
        label1.model().set(xText, GeomUtil.yOfLine(xText, xs[0], ys[0], k1), "0.618");
        label1.plot();
        
        Label label2 = addChild(new Label());
        label2.setFont(LookFeel.getCurrent().axisFont);
        label2.setForeground(color);
        label2.model().set(xText, GeomUtil.yOfLine(xText, xs[0], ys[0], k2), "0.5");
        label2.plot();

        Label label3 = addChild(new Label());
        label3.setFont(LookFeel.getCurrent().axisFont);
        label3.setForeground(color);
        label3.model().set(xText, GeomUtil.yOfLine(xText, xs[0], ys[0], k3), "0.382");
        label3.plot();

        PathWidget pathWidget1 = addChild(new PathWidget());
        pathWidget1.setForeground(color);
        plotLine(xs[0], ys[0], k1, pathWidget1.getPath());

        PathWidget pathWidget2 = addChild(new PathWidget());
        pathWidget2.setForeground(color);
        plotLine(xs[0], ys[0], k2, pathWidget2.getPath());

        PathWidget pathWidget3 = addChild(new PathWidget());
        pathWidget3.setForeground(color);
        plotLine(xs[0], ys[0], k3, pathWidget3.getPath());
    }
    
}





