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
import org.aiotrade.charting.widget.HeavyPathWidget;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.charting.widget.LineSegment;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.charting.chart.PolyLineChart.Model;
import org.aiotrade.charting.laf.LookFeel;

/**
 *
 * @author Caoyuan Deng
 */
public class PolyLineChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        Var var;
        
        public void set(Var var) {
            this.var = var;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        Color color = LookFeel.getCurrent().getChartColor(getDepth());
        setForeground(color);
        
        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final LineSegment template = new LineSegment();
        float y1 = Float.NaN;   // for prev
        float y2 = Float.NaN;   // for curr
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            float value = Float.NaN;
            float max = -Float.MAX_VALUE;
            float min = +Float.MAX_VALUE;
            for (int i = 0; i < nBarsCompressed; i++) {
                final long time = tb(bar + i);
                final SerItem item = ser.getItem(time);
                if (item != null) {
                    value = item.getFloat(model.var);
                    max = Math.max(max, value);
                    min = Math.min(min, value);
                }
            }
            
            if (! Float.isNaN(value)) {
                template.setForeground(color);
                
                y2 = yv(value);
                if (nBarsCompressed > 1) {
                    /** draw a vertical line to cover the min to max */
                    final float x = xb(bar);
                    template.model().set(x, yv(min), x, yv(max));
                } else {
                    if (! Float.isNaN(y1)) {
                        /**
                         * x1 shoud be decided here, it may not equal prev x2:
                         * think about the case of on calendar day mode
                         */
                        final float x1 = xb(bar - nBarsCompressed);
                        final float x2 = xb(bar);
                        template.model().set(x1, y1, x2, y2);
                        
                        if (x2 % MARK_INTERVAL == 0) {
                            addMarkPoint((int)x2, (int)y2);
                        }
                        
                    }
                }
                y1 = y2;
                
                template.plot();
                heavyPathWidget.appendFrom(template);
            }
        }
        
    }
    
}