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
package org.aiotrade.charting.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;
import java.util.Set;
import org.aiotrade.math.timeseries.plottable.Plot;
import org.aiotrade.charting.chart.ChartFactory;
import org.aiotrade.charting.chart.GridChart;
import org.aiotrade.charting.chart.ProfileChart;
import org.aiotrade.charting.chart.GradientChart;
import org.aiotrade.charting.chart.StickChart;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.charting.chart.Chart;
import org.aiotrade.charting.view.pane.Pane;

/**
 *
 * @author Caoyuan Deng
 */
public class IndicatorChartView extends ChartView {
    
    public IndicatorChartView() {
    }
    
    public IndicatorChartView(ChartingController controller, Ser mainSer) {
        init(controller, mainSer);
    }
    
    @Override
    public void init(ChartingController controller, Ser mainSer) {
        super.init(controller, mainSer);
    }
    
    /**
     * Layout of IndicatorView
     *
     * title pane is intersect on chart pane's north
     * +----------------------------------------------------+-----+
     * |    title (0,0)                                     |     |
     * +----------------------------------------------------+     |
     * |    mainLayeredPane (0, 0)                          |     |
     * |    chartPane                                       |axisy|
     * |    drawingPane                                     |(1,0)|
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * |                                                    |     |
     * +----------------------------------------------------+-----+
     * |    axisx                                                 |
     * +----------------------------------------------------------+
     */
    protected void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.anchor = gbc.CENTER;
        gbc.fill = gbc.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100 - 100 / 6.18;
        add(glassPane, gbc);
        
        gbc.anchor = gbc.CENTER;
        gbc.fill = gbc.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100 - 100 / 6.18;
        add(mainLayeredPane, gbc);
        
        gbc.anchor = gbc.CENTER;
        gbc.fill = gbc.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 100;
        add(axisYPane, gbc);
        
        gbc.anchor = gbc.SOUTH;
        gbc.fill = gbc.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = gbc.RELATIVE;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 0;
        add(divisionPane, gbc);
    }
    
    
    protected void putChartsOfMainSer() {
        int depth = Pane.DEPTH_CHART_BEGIN;
        int depthGradient = Pane.DEPTH_GRADIENT_BEGIN;
        
        for (Ser ser : getAllSers()) {
            /** add charts */
            for (Var<?> var : ser.varSet()) {
                Set<Var<?>> chartVars = new HashSet<Var<?>>();
                Chart<?> chart = ChartFactory.createVarChart(chartVars, var);
                if (chart != null) {
                    mainSerChartMapVars.put(chart, chartVars);
                    
                    chart.set(mainChartPane, ser);
                    
                    if (chart instanceof GradientChart || chart instanceof ProfileChart) {
                        chart.setDepth(depthGradient--);
                    } else if (chart instanceof StickChart) {
                        chart.setDepth(-8);
                    } else {
                        chart.setDepth(depth++);
                    }
                    
                    mainChartPane.putChart(chart);
                }
            }
            
            /** plot grid */
            Float[] grids = ((Indicator)mainSer).getGrids();
            if (grids != null && grids.length > 0) {
                GridChart gridChart = new GridChart();
                
                gridChart.model().set(grids, GridChart.Direction.Horizontal);
                gridChart.set(mainChartPane, null, Pane.DEPTH_DRAWING);
                
                mainChartPane.putChart(gridChart);
            }
        }
    }
    
    @Override
    public void computeMaxMin() {
        float minValue1 = +Float.MAX_VALUE;
        float maxValue1 = -Float.MAX_VALUE;
        
        for (int i = 1; i <= getNBars(); i++) {
            long time = tb(i);
            SerItem item = mainSer.getItem(time);
            if (item != null) {
                for (Var var : mainSer.varSet()) {
                    if (var.getPlot() != Plot.None) {
                        float value = item.getFloat(var);
                        if (!Float.isNaN(value)) {
                            maxValue1 = Math.max(maxValue1, value);
                            minValue1 = Math.min(minValue1, value);
                        }
                    }
                }
            }
        }
        
        if (maxValue1 == minValue1) {
            maxValue1 += 1;
        }
        
        setMaxMinValue(maxValue1, minValue1);
    }
    
    @Override
    public void popupToDesktop() {
        ChartView popupView = new PopupIndicatorChartView(getController(), getMainSer());
        final boolean alwaysOnTop = true;
        final Dimension dimension = new Dimension(getWidth(), 200);
        
        getController().popupViewToDesktop(popupView, dimension, alwaysOnTop, false);
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}


