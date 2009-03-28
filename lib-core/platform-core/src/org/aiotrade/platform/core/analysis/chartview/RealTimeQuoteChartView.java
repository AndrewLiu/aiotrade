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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Calendar;
import java.util.Date;
import org.aiotrade.charting.chart.GridChart;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.math.timeseries.SerChangeEvent;
import org.aiotrade.charting.chart.QuoteChart;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.pane.Pane;
import org.aiotrade.math.timeseries.QuoteItem;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.util.swing.GBC;

/**
 *
 * @author Caoyuan Deng
 */
public class RealTimeQuoteChartView extends AbstractQuoteChartView {

    /** all RealtimeQuoteChartView instances share the same type */
    private static QuoteChart.Type quoteChartType;
    private float prevClose = Float.NaN;
    private Float[] gridValues;
    private QuoteSer tickerSer;
    private Calendar cal = Calendar.getInstance();
    private Market market;

    public RealTimeQuoteChartView() {
    }

    public RealTimeQuoteChartView(ChartingController controller, QuoteSer quoteSer) {
        init(controller, quoteSer);
    }

    @Override
    public void init(ChartingController controller, Ser mainSer) {
        super.init(controller, mainSer);

        getController().setAutoScrollToNewData(false);
        getController().setOnCalendarMode(false);
        getController().growWBar(-2);
        axisYPane.setSymmetricOnMiddleValue(true);

        quoteChartType = quoteChartType.Line;

        market = sec.getMarket();
        tickerSer = sec.getTickerSer();
        assert (tickerSer != null);
        tickerSer.addSerChangeListener(serChangeListener);
    }

    protected void initComponents() {
        glassPane.setUsingInstantTitleValue(true);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 618;
        add(glassPane, gbc);

        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 618;
        add(mainLayeredPane, gbc);

        gbc.anchor = GBC.CENTER;
        gbc.fill = GBC.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 100;
        add(axisYPane, gbc);

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
    protected void putChartsOfMainSer() {
        super.putChartsOfMainSer();

        // draw prevClose value grid
        GridChart prevCloseGrid = new GridChart();
        prevCloseGrid.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT);
        gridValues = new Float[]{prevClose};
        prevCloseGrid.model().set(gridValues, GridChart.Direction.Horizontal);
        mainChartPane.putChart(prevCloseGrid);
    }

    @Override
    public void computeMaxMin() {
        super.computeMaxMin();

        float minValue1 = +Float.MAX_VALUE;
        float maxValue1 = -Float.MAX_VALUE;
        if (!Float.isNaN(prevClose)) {
            minValue1 = getMinValue();
            maxValue1 = getMaxValue();
            float maxDelta = Math.max(Math.abs(maxValue1 - prevClose), Math.abs(minValue1 - prevClose));
            maxValue1 = prevClose + maxDelta;
            minValue1 = prevClose - maxDelta;
            setMaxMinValue(maxValue1, minValue1);
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

    public void setPrevClose(float prevClose) {
        this.prevClose = prevClose;
        gridValues[0] = prevClose;
        mainChartPane.setReferCursorValue(prevClose);
        glassPane.setReferCursorValue(prevClose);
    }

    @Override
    public void popupToDesktop() {
        ChartView popupView = new RealTimeQuoteChartView(getController(), getQuoteSer());
        popupView.setInteractive(false);
        final Dimension dimension = new Dimension(200, 150);
        final boolean alwaysOnTop = true;

        getController().popupViewToDesktop(popupView, dimension, alwaysOnTop, false);
    }

    @Override
    protected void updateView(SerChangeEvent evt) {
        Object lastObj = evt.getLastObject();

        long lastOccurredTime = masterSer.lastOccurredTime();
        if (lastObj != null && lastObj instanceof Ticker) {
            Ticker ticker = (Ticker) lastObj;

            float percentValue = ticker.getChangeInPercent();

            String strValue = String.format("%+3.2f", percentValue) + "%  " + ticker.get(Ticker.LAST_PRICE);

            Color color = percentValue >= 0 ? LookFeel.getCurrent().getPositiveColor() : LookFeel.getCurrent().getNegativeColor();

            getGlassPane().updateInstantValue(strValue, color);
            setPrevClose(ticker.get(Ticker.PREV_CLOSE));

            long time = ticker.getTime();
            if (time >= lastOccurredTime) {
                lastOccurredTime = time;
            }
        }

        if (lastOccurredTime == 0) {
            cal.setTime(new Date());
            lastOccurredTime = cal.getTimeInMillis();
        }

        adjustLeftSideRowToMarketOpenTime(lastOccurredTime);
    }

    private void adjustLeftSideRowToMarketOpenTime(long time) {
        long openTime = market.openTime(time);
        long closeTime = market.closeTime(time);

        int begRow = masterSer.rowOfTime(openTime);
        int nBars = getNBars();
        int endRow = begRow + nBars - 1;

        if (Float.isNaN(prevClose)) {
            // @todo get precise prev *day* close
            QuoteItem prevRow = (QuoteItem) masterSer.getItemByRow(begRow - 1);
            if (prevRow != null) {
                prevClose = prevRow.getClose();
                gridValues[0] = prevClose;
            }
        }

        int lastOccurredRow = masterSer.lastOccurredRow();
        controller.setCursorByRow(lastOccurredRow, endRow, true);
        //controller.updateViews();
    }
}


