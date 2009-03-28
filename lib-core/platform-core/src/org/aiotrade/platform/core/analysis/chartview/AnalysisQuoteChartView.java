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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.view.WithDrawingPaneHelper;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.computable.Option;
import org.aiotrade.charting.chart.QuoteChart;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.platform.core.analysis.indicator.QuoteCompareIndicator;
import org.aiotrade.math.timeseries.computable.Opt;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.charting.view.pane.XControlPane;
import org.aiotrade.charting.view.pane.YControlPane;
import org.aiotrade.charting.view.pane.Pane;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.util.swing.GBC;

/**
 *
 * @author Caoyuan Deng
 */

public class AnalysisQuoteChartView extends AbstractQuoteChartView implements WithDrawingPane {
    
    /** all AnalysisQuoteChartView instances share the same type */
    private static QuoteChart.Type quoteChartType;
    
    private Map<QuoteCompareIndicator, QuoteChart> compareIndicatorMapChart;
    
    /**
     * To avoid null withDrawingPaneHelper when getSelectedDrawing called by other
     * threads (such as dataLoadServer is running and fire a SerChangeEvent
     * to force a updateView() calling), we should create withDrawingPaneHelper here
     * (this will makes it be called before the code:
     *     this.mainSer.addSerChangeListener(serChangeListener);
     * in it's super's constructor: @See:ChartView#ChartView(ChartViewContainer, Ser)
     */
    private WithDrawingPaneHelper withDrawingPaneHelper = new WithDrawingPaneHelper(this);
    
    public AnalysisQuoteChartView() {
    }
    
    public AnalysisQuoteChartView(ChartingController controller, QuoteSer quoteSer) {
        init(controller, quoteSer);
    }
    
    @Override
    public void init(ChartingController controller, Ser quoteSer) {
        super.init(controller, quoteSer);
        
        quoteChartType = LookFeel.getCurrent().getQuoteChartType();
        
        compareIndicatorMapChart = new HashMap();
    }
    
    protected void initComponents() {
        xControlPane = new XControlPane(this, mainChartPane);
        xControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT));
        
        yControlPane = new YControlPane(this, mainChartPane);
        yControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT));
        
        /** begin to set the layout: */
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        /**
         * !NOTICE be ware of the components added order:
         * 1. add xControlPane, it will partly cover glassPane in SOUTH,
         * 2. add glassPane, it will exactly cover mainLayeredPane
         * 3. add mainLayeredPane.
         *
         * After that, xControlPane can accept its self mouse events, and so do
         * glassPane except the SOUTH part covered by xControlPane.
         *
         * And glassPane will forward mouse events to whom it covered.
         * @see GlassPane#processMouseEvent(MouseEvent) and
         *      GlassPane#processMouseMotionEvent(MouseEvent)
         */
        
        gbc.anchor = GBC.SOUTH;
        gbc.fill = GBC.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 0;
        add(xControlPane, gbc);
        
        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100 - 100 / 6.18;
        add(glassPane, gbc);
        
        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100 - 100 / 6.18;
        add(mainLayeredPane, gbc);
        
        /** add the yControlPane first, it will cover axisYPane partly in SOUTH */
        gbc.anchor = GBC.SOUTH;
        gbc.fill = GBC.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        add(yControlPane, gbc);
        
        /**
         * add the axisYPane in the same grid as yControlPane then, it will be
         * covered by yControlPane partly in SOUTH
         */
        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 100;
        add(axisYPane, gbc);
        
        /** add axisXPane and dividentPane across 2 gridwidth horizontally, */
        
        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = GBC.RELATIVE;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 0;
        add(axisXPane, gbc);
        
        gbc.anchor = GBC.SOUTH;
        gbc.fill = GBC.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = GBC.RELATIVE;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 0;
        add(divisionPane, gbc);
    }
    
    @Override
    protected void computeGeometry() {
        super.computeGeometry();
        
        if (getCompareIndicators().size() > 0) {
            refreshQuoteCompareSer();
            calcMaxMinWithComparingQuotes();
        }
    }
    
    public QuoteChart.Type getQuoteChartType() {
        return quoteChartType;
    }
    
    public void switchQuoteChartType(QuoteChart.Type type) {
        switchAllQuoteChartType(type);
        
        repaint();
    }
    
    public static void switchAllQuoteChartType(QuoteChart.Type type) {
        quoteChartType = internal_switchAllQuoteChartType(quoteChartType, type);
    }
    
    private void refreshQuoteCompareSer() {
        List<Opt> optsForCompareIndicator = new ArrayList();
        
        optsForCompareIndicator.add(new Option("Begin of Time Frame", rb(1)));
        optsForCompareIndicator.add(new Option("End of Time Frame",   rb(getNBars())));
        optsForCompareIndicator.add(new Option("Max Value", getMaxValue()));
        optsForCompareIndicator.add(new Option("Min Value", getMinValue()));
        
        for (Indicator ser : getCompareIndicators()) {
            ser.setOpts(optsForCompareIndicator);
        }
    }
    
    /** calculate maxValue and minValue again, including comparing quotes */
    private void calcMaxMinWithComparingQuotes() {
        float maxValue1 = getMaxValue();
        float minValue1 = getMinValue();
        for (QuoteCompareIndicator ser : getCompareIndicators()) {
            for (int i = 1; i <= getNBars(); i++) {
                long time = tb(i);
                SerItem item = ser.getItem(time);
                if (item != null) {
                    float compareHigh = item.getFloat(ser.high);
                    float compareLow  = item.getFloat(ser.low);
                    if (!Float.isNaN(compareHigh) && !Float.isNaN(compareLow) && !(compareHigh * compareLow == 0) ) {
                        maxValue1 = Math.max(maxValue1, compareHigh);
                        minValue1 = Math.min(minValue1, compareLow);
                    }
                }
            }
        }
        
        
        if (maxValue1 == minValue1) {
            maxValue1 += 1;
        }
        
        setMaxMinValue(maxValue1, minValue1);
    }
    
    public Set<QuoteCompareIndicator> getCompareIndicators() {
        return compareIndicatorMapChart.keySet();
    }
    
    public Map<QuoteCompareIndicator, QuoteChart> getCompareIndicatorMapChart() {
        return compareIndicatorMapChart;
    }
    
    public void addQuoteCompareChart(QuoteCompareIndicator ser) {
        ser.addSerChangeListener(serChangeListener);
        
        QuoteChart chart = new QuoteChart();
        compareIndicatorMapChart.put(ser, chart);
        
        int depth = Pane.DEPTH_CHART_BEGIN + compareIndicatorMapChart.size();
        
        chart.model().set(
                ser.open,
                ser.high,
                ser.low,
                ser.close);
        
        chart.set(mainChartPane, ser, depth);
        mainChartPane.putChart(chart);
        
        repaint();
    }
    
    public void removeQuoteCompareChart(QuoteCompareIndicator ser) {
        ser.removeSerChangeListener(serChangeListener);
        
        QuoteChart chart = compareIndicatorMapChart.get(ser);
        if (chart != null) {
            mainChartPane.removeChart(chart);
            compareIndicatorMapChart.remove(ser);
            
            repaint();
        }
    }
    
    /**
     * implement of WithDrawingPane
     * -------------------------------------------------------
     */
    
    public DrawingPane getSelectedDrawing() {
        return withDrawingPaneHelper.getSelectedDrawing();
    }
    
    public void setSelectedDrawing(DrawingPane drawing) {
        withDrawingPaneHelper.setSelectedDrawing(drawing);
    }
    
    public void addDrawing(DrawingDescriptor descriptor, DrawingPane drawing) {
        withDrawingPaneHelper.addDrawing(descriptor, drawing);
    }
    
    public void deleteDrawing(DrawingDescriptor descriptor) {
        withDrawingPaneHelper.deleteDrawing(descriptor);
    }
    
    public Map<DrawingDescriptor, DrawingPane> getDescriptorMapDrawing() {
        return withDrawingPaneHelper.getDescriptorMapDrawing();
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
    
}

