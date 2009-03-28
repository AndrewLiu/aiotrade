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
import java.awt.geom.GeneralPath;
import java.util.List;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.charting.chart.AbstractChart;
import org.aiotrade.lib.charting.chart.util.ValuePoint;
import org.aiotrade.lib.charting.chart.handledchart.StraightLineSegmentChart.Model;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.widget.PathWidget;

/**
 *
 * @author Caoyuan Deng
 */
public class StraightLineSegmentChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        List<ValuePoint> points;
        
        public void set(List<ValuePoint> points) {
            this.points = points;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        final PathWidget pathWidget = addChild(new PathWidget());
        pathWidget.setForeground(color);
        final GeneralPath path = pathWidget.getPath();
        final int nPoints = model.points.size();
        for (int i = 0; i < nPoints - 1; i++) {
            final float x1 = xb(bt(model.points.get(i).t));
            final float x2 = xb(bt(model.points.get(i + 1).t));
            final float y1 = yv(model.points.get(i).v);
            final float y2 = yv(model.points.get(i + 1).v);
            
            final float dx = x2 - x1;
            final float dy = y2 - y1;
            
            final float k = dx == 0 ? 1 : dy / dx;
            
            plotLineSegment(x1, y1, x2, y2, path);
        }
    }
    
}


