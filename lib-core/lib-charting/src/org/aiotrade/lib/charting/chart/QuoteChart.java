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
package org.aiotrade.lib.charting.chart;

import java.awt.Color;
import org.aiotrade.lib.charting.widget.CandleBar;
import org.aiotrade.lib.charting.widget.LineSegment;
import org.aiotrade.lib.charting.widget.HeavyPathWidget;
import org.aiotrade.lib.charting.widget.OhlcBar;
import org.aiotrade.lib.charting.widget.PathWidget;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.math.timeseries.SerItem;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.charting.chart.QuoteChart.Model;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.WithQuoteChart;
import org.aiotrade.lib.charting.view.pane.Pane;


/**
 *
 * @author Caoyuan Deng
 */
public class QuoteChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        Var openVar;
        Var highVar;
        Var lowVar;
        Var closeVar;
        
        public void set(Var openVar, Var highVar, Var lowVar, Var closeVar) {
            this.openVar = openVar;
            this.highVar = highVar;
            this.lowVar = lowVar;
            this.closeVar = closeVar;
        }
    }
    
    private Color positiveColor;
    private Color negativeColor;
    
    /**
     * Type will be got from the static property quoteChartType of view and we
     * should consider the case of view's repaint() being called, so we do not
     * include it in Model.
     */
    public enum Type {
        Candle,
        Ohlc,
        Line
    };
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        
        if (getDepth() == Pane.DEPTH_DEFAULT) {
            positiveColor = LookFeel.getCurrent().getPositiveColor();
            negativeColor = LookFeel.getCurrent().getNegativeColor();
        } else {
            /** for comparing quotes charts */
            positiveColor = LookFeel.getCurrent().getChartColor(getDepth());
            negativeColor = positiveColor;
        }
        
        Color color = positiveColor;
        setForeground(color);
        
        final Type type = ((WithQuoteChart)datumPlane.getView()).getQuoteChartType();
        switch (type) {
            case Candle:
            case Ohlc:
                plotCandleOrOhlcChart(type);
                break;
            case Line:
                plotLineChart();
                break;
            default:
        }
        
    }
    
    private void plotCandleOrOhlcChart(Type type) {
        final Model model = model();
        
        /**
         * @NOTICE
         * re-create and re-add children each time, so the children will release 
         * its resource when reset(); 
         */
        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final PathWidget template = type == Type.Candle ? new CandleBar() : new OhlcBar();
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            /**
             * @TIPS:
             * use NaN to test if value has been set at least one time
             */
            float open  = Float.NaN;
            float close = Float.NaN;
            float high  = -Float.MAX_VALUE;
            float low   = +Float.MAX_VALUE;
            for (int i = 0; i < nBarsCompressed; i++) {
                final long time = tb(bar + i);
                final SerItem item = ser.getItem(time);
                
                if (item != null && item.getFloat(model.closeVar) != 0) {
                    if (Float.isNaN(open)) {
                        /** only get the first open as compressing period's open */
                        open = item.getFloat(model.openVar);
                    }
                    high  = Math.max(high, item.getFloat(model.highVar));
                    low   = Math.min(low,  item.getFloat(model.lowVar));
                    close = item.getFloat(model.closeVar);
                }
            }
            
            if (! Float.isNaN(close) && close != 0) {
                Color color = close >= open ? positiveColor : negativeColor;
                
                final float yOpen  = yv(open);
                final float yHigh  = yv(high);
                final float yLow   = yv(low);
                final float yClose = yv(close);
                
                switch (type) {
                    case Candle:
                        boolean fillBar = LookFeel.getCurrent().isFillBar();
                        ((CandleBar)template).model().set(xb(bar), yOpen, yHigh, yLow, yClose, wBar, fillBar || close < open ? true : false);
                        break;
                    case Ohlc:
                        ((OhlcBar)template).model().set(xb(bar), yOpen, yHigh, yLow, yClose, wBar);
                        break;
                    default:
                }
                template.setForeground(color);
                template.plot();
                heavyPathWidget.appendFrom(template);
            }
        }
        
    }
    
    private void plotLineChart() {
        final Model model = model();
        
        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final LineSegment template = new LineSegment();
        float y1 = Float.NaN;   // for prev
        float y2 = Float.NaN;   // for curr
        for (int bar = 1; bar <= nBars; bar++) {
            
            /**
             * @TIPS:
             * use NaN to test if value has been set at least one time
             */
            float open  = Float.NaN;
            float close = Float.NaN;
            float max = -Float.MAX_VALUE;
            float min = +Float.MAX_VALUE;
            for (int i = 0; i < nBarsCompressed; i++) {
                final long time = tb(bar + i);
                final SerItem item = ser.getItem(time);
                if (item != null && item.getFloat(model.closeVar) != 0) {
                    if (Float.isNaN(open)) {
                        /** only get the first open as compressing period's open */
                        open = item.getFloat(model.openVar);
                    }
                    close = item.getFloat(model.closeVar);
                    max = Math.max(max, close);
                    min = Math.min(min, close);
                }
            }
            
            if (! Float.isNaN(close) && close != 0) {
                Color color = close >= open ? positiveColor : negativeColor;
                
                y2 = yv(close);
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
                    }
                }
                y1 = y2;
                
                template.setForeground(color);
                template.plot();
                heavyPathWidget.appendFrom(template);
            }
        }
        
    }
    
}
