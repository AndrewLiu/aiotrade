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
import org.aiotrade.charting.chart.handledchart.FibonacciRetracementsChart.Model;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.Label;
import org.aiotrade.charting.widget.PathWidget;


/**
 *
 * @author Caoyuan Deng
 */
public class FibonacciRetracementsChart extends AbstractChart<Model> {
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
        
        float y01 = ys[0];
        float y02 = ys[0] + interval * 0.236f;
        float y03 = ys[0] + interval * 0.382f;
        float y04 = ys[0] + interval * 0.500f;
        float y05 = ys[0] + interval * 0.618f;
        float y06 = ys[0] + interval * 0.763f;
        float y07 = ys[1];
        float y08 = ys[0] + interval * 1.618f;
        float y09 = ys[0] + interval * 2.0f;
        float y10 = ys[0] + interval * 2.618f;
        float y11 = ys[0] + interval * 3.0f;
        float y12 = ys[0] + interval * 4.237f;
        
        Label label1 = addChild(new Label());
        label1.setFont(LookFeel.getCurrent().axisFont);
        label1.setForeground(color);
        label1.model().set(xs[0], y02 - 2, "23.6%");
        label1.plot();
        
        Label label2 = addChild(new Label());
        label2.setFont(LookFeel.getCurrent().axisFont);
        label2.setForeground(color);
        label2.model().set(xs[0], y03 - 2, "38.2%");
        label2.plot();
        
        Label label3 = addChild(new Label());
        label3.setFont(LookFeel.getCurrent().axisFont);
        label3.setForeground(color);
        label3.model().set(xs[0], y04 - 2, "50.0%");
        label3.plot();

        Label label4 = addChild(new Label());
        label4.setFont(LookFeel.getCurrent().axisFont);
        label4.setForeground(color);
        label4.model().set(xs[0], y05 - 2, "61.8%");
        label4.plot();

        Label label5 = addChild(new Label());
        label5.setFont(LookFeel.getCurrent().axisFont);
        label5.setForeground(color);
        label5.model().set(xs[0], y06 - 2, "76.3%");
        label5.plot();

        Label label6 = addChild(new Label());
        label6.setFont(LookFeel.getCurrent().axisFont);
        label6.setForeground(color);
        label6.model().set(xs[0], y07 - 2, "100%");
        label6.plot();

        Label label7 = addChild(new Label());
        label7.setFont(LookFeel.getCurrent().axisFont);
        label7.setForeground(color);
        label7.model().set(xs[0], y08 - 2, "161.8%");
        label7.plot();

        Label label8 = addChild(new Label());
        label8.setFont(LookFeel.getCurrent().axisFont);
        label8.setForeground(color);
        label8.model().set(xs[0], y09 - 2, "200%");
        label8.plot();

        Label label9 = addChild(new Label());
        label9.setFont(LookFeel.getCurrent().axisFont);
        label9.setForeground(color);
        label9.model().set(xs[0], y10 - 2, "261.8%");
        label9.plot();

        Label label10 = addChild(new Label());
        label10.setFont(LookFeel.getCurrent().axisFont);
        label10.setForeground(color);
        label10.model().set(xs[0], y11 - 2, "300%");
        label10.plot();
        
        Label label11 = addChild(new Label());
        label11.setFont(LookFeel.getCurrent().axisFont);
        label11.setForeground(color);
        label11.model().set(xs[0], y12 - 2, "423.7%");
        label11.plot();
        
        PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        GeneralPath path = pathWidget.getPath();
        float x1 = xb(0);
        float x2;
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            x2 = xb(bar);
            if (x2 >= xmin && x2 <= xmax) {
                path.moveTo(x1, y01);
                path.lineTo(x2, y01);
                path.moveTo(x1, y02);
                path.lineTo(x2, y02);
                path.moveTo(x1, y03);
                path.lineTo(x2, y03);
                path.moveTo(x1, y04);
                path.lineTo(x2, y04);
                path.moveTo(x1, y05);
                path.lineTo(x2, y05);
                path.moveTo(x1, y06);
                path.lineTo(x2, y06);
                path.moveTo(x1, y07);
                path.lineTo(x2, y07);
                path.moveTo(x1, y08);
                path.lineTo(x2, y08);
                path.moveTo(x1, y09);
                path.lineTo(x2, y09);
                path.moveTo(x1, y10);
                path.lineTo(x2, y10);
                path.moveTo(x1, y11);
                path.lineTo(x2, y11);
                path.moveTo(x1, y12);
                path.lineTo(x2, y12);
            }
            
            /**
             * should avoid the 1 point intersect at the each path's end point,
             * especially in XOR mode
             */
            x1 = x2 + 1;
            
        }
        
    }
    
}



