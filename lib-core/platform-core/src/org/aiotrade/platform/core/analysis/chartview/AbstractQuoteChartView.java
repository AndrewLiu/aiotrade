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
package org.aiotrade.platform.core.analysis.chartview;

import java.util.HashSet;
import java.util.Set;
import org.aiotrade.lib.charting.chart.QuoteChart;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.ChartingController;
import org.aiotrade.lib.charting.view.WithQuoteChart;
import org.aiotrade.lib.charting.view.pane.Pane;
import org.aiotrade.lib.charting.view.scalar.Scalar;
import org.aiotrade.lib.charting.view.scalar.LgScalar;
import org.aiotrade.lib.charting.view.scalar.LinearScalar;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.QuoteItem;
import org.aiotrade.lib.math.timeseries.QuoteSer;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.math.timeseries.plottable.Plot;
import org.aiotrade.platform.core.sec.Sec;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractQuoteChartView extends ChartView implements WithQuoteChart {

    private QuoteChart quoteChart;
    protected float maxVolume, minVolume;
    protected Sec sec;

    public AbstractQuoteChartView() {
    }

    public AbstractQuoteChartView(ChartingController controller, QuoteSer quoteSer) {
        init(controller, mainSer);
    }

    @Override
    public void init(ChartingController controller, Ser mainSer) {
        super.init(controller, mainSer);
        sec = (Sec) controller.getContents().getSerProvider();
        if (axisXPane != null) {
            axisXPane.setTimeZone(sec.getMarket().getTimeZone());
        }
    }

    public QuoteSer getQuoteSer() {
        return (QuoteSer) mainSer;
    }

    protected void putChartsOfMainSer() {
        quoteChart = new QuoteChart();

        Set<Var<?>> vars = new HashSet<Var<?>>();
        mainSerChartMapVars.put(quoteChart, vars);
        for (Var<?> var : mainSer.varSet()) {
            if (var.getPlot() == Plot.Quote) {
                vars.add(var);
            }
        }

        quoteChart.model().set(
                getQuoteSer().getOpen(),
                getQuoteSer().getHigh(),
                getQuoteSer().getLow(),
                getQuoteSer().getClose());

        quoteChart.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT);
        mainChartPane.putChart(quoteChart);
    }

    @Override
    public void computeMaxMin() {
        float minValue1 = +Float.MAX_VALUE;
        float maxValue1 = -Float.MAX_VALUE;

        /** minimum volume should be 0 */
        minVolume = 0;
        maxVolume = -Float.MAX_VALUE;

        for (int i = 1; i <= getNBars(); i++) {
            long time = tb(i);
            QuoteItem item = (QuoteItem) mainSer.getItem(time);
            if (item != null && item.getClose() > 0) {
                maxValue1 = Math.max(maxValue1, item.getHigh());
                minValue1 = Math.min(minValue1, item.getLow());
                maxVolume = Math.max(maxVolume, item.getVolume());
            }
        }

        if (maxVolume == 0) {
            maxVolume = 1;
        }

        if (maxVolume == minVolume) {
            maxVolume += 1;
        }

        if (maxValue1 == minValue1) {
            maxValue1 *= 1.05f;
            minValue1 *= 0.95f;
        }

        setMaxMinValue(maxValue1, minValue1);
    }

    public float getMaxVolume() {
        return maxVolume;
    }

    public float getMinVolume() {
        return minVolume;
    }

    public QuoteChart getQuoteChart() {
        return quoteChart;
    }

    public void swithScalarType() {
        Scalar.Type type = getMainChartPane().getValueScalar().getType();
        if (type == Scalar.Type.Linear) {
            setValueScalar(new LgScalar());
        } else {
            setValueScalar(new LinearScalar());
        }
    }

    protected static QuoteChart.Type internal_switchAllQuoteChartType(QuoteChart.Type originalType, QuoteChart.Type targetType) {
        QuoteChart.Type newType = null;

        if (targetType != null) {
            newType = targetType;
        } else {
            switch (originalType) {
                case Candle:
                    newType = QuoteChart.Type.Ohlc;
                    break;
                case Ohlc:
                    newType = QuoteChart.Type.Line;
                    break;
                case Line:
                    newType = QuoteChart.Type.Candle;
                    break;
                default:
            }
        }

        return newType;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}


