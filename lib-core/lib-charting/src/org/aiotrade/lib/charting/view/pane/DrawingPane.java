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
package org.aiotrade.lib.charting.view.pane;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import org.aiotrade.lib.math.PersistenceManager;
import org.aiotrade.lib.charting.chart.Chart;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.chart.handledchart.HandledChart;
import org.aiotrade.lib.charting.chart.util.ValuePoint;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.lib.charting.widget.PathWidget;


/**
 *
 * @author Caoyuan Deng
 */
public class DrawingPane extends Pane {
    private boolean givenHandledChartsLoaded;
    
    private HandledChart selectedHandledChart;
    
    private String layerName;
    private DrawingDescriptor descriptor;
    
    private boolean activated;
    
    public DrawingPane(ChartView view, DatumPlane datumPlane, DrawingDescriptor descriptor) {
        super(view, datumPlane);
        this.descriptor = descriptor;
        this.layerName = descriptor.getDisplayName();
        
        setOpaque(false);
        setRenderStrategy(RenderStrategy.NoneBuffer);
        setLayout(null);
    }
    
    /**
     * load given handledChartMap by descriptor, this method could be called
     * lazily, but should before this pane is set activate;
     *
     * @see #setActive(boolean)
     */
    private void loadGivenHandledCharts() {
        final Map<HandledChart, List<ValuePoint>> handledChartsWithPoints = descriptor.getHandledChartMapPoints();
        for (HandledChart hc : handledChartsWithPoints.keySet()) {
            hc.init(this, handledChartsWithPoints.get(hc));
        }
        givenHandledChartsLoaded = true;
    }
    
    public HandledChart getSelectedHandledChart() {
        return selectedHandledChart;
    }
    
    @Override
    protected void processMouseEvent(MouseEvent e) {
        /** fire to listeners */
        super.processMouseEvent(e);
        
        if (selectedHandledChart != null && !selectedHandledChart.isAccomplished()) {
            /**
             * there is one handledChart still under accompishing, let it to be
             * accomplished before do any other things here.
             *
             * @NOTICE
             * an unAcccomplished chart won't be put to descriptor. so you can not
             * find it in descriptor.getHandledChartMap().keySet()
             */
            return;
        }
        
        /** check if any selection event happened, and set selectedHandledChart here */
        if (e.getClickCount() == 1) {
            HandledChart selectedOne = null;
            for (HandledChart handledChart : descriptor.getHandledChartMapPoints().keySet()) {
                if (handledChart.getChart().isSelected()) {
                    selectedOne = handledChart;
                    break;
                }
            }
            
            setSelectedHandledChart(selectedOne);
        }
    }
    
    /**
     * set selected handled chart and make passivate/activate decision here
     *
     *
     * @param hanldedChart to be set selected, <code>null</code> means to clear it.
     */
    public void setSelectedHandledChart(HandledChart handledChart) {
        HandledChart previousOne = this.selectedHandledChart;
        this.selectedHandledChart = handledChart;
        
        if (previousOne != null) {
            previousOne.passivate();
        }
        
        if (selectedHandledChart != null) {
            selectedHandledChart.activate();
        }
        
        boolean selectionChanged = selectedHandledChart != previousOne;
        if (selectionChanged) {
            if (selectedHandledChart != null) {
                /**
                 * when ready to drawing, move this to front, this makes the drawing
                 * smoothly and the chart especially the handles in front of charts
                 * of mainChartPane.
                 */
                view.getMainLayeredPane().moveToFront(this);
            } else {
                /**
                 * if clear selectedHandledChart, move this to back, thus this pane
                 * will be in back of mainChartPane and the handled charts in this
                 * pane will not overlap mainChartPane's charts.
                 */
                view.getMainLayeredPane().moveToBack(this);
            }
            
            view.repaint();
        }
    }
    
    public void removeSelectedHandledChart() {
        if (selectedHandledChart != null && selectedHandledChart.isActivated()) {
            removeChart(selectedHandledChart.getChart());
            /**
             * should remember to removeMouseAdapterOnPane that was
             * added passively by handledChart
             */
            selectedHandledChart.removeMouseAdapterOnPane();
            
            descriptor.removeHandledChart(selectedHandledChart);
            PersistenceManager.getDefault().saveContents(view.getController().getContents());
            
            setSelectedHandledChart(null);
        }
    }
    
    public void accomplishedHandledChartChanged(HandledChart handledChart) {
        assert handledChart.isAccomplished() : "Only accomplished handledChart will call me!";
        Chart chart = handledChart.getChart();
        if (!containsChart(chart)) {
            putChart(chart);
        }
        view.repaint();
        
        /** the chart may have got new handlesPoints, so, put them to descriptor anyway and save */
        descriptor.putHandledChart(handledChart, handledChart.getCurrentHandlesPoints());
        PersistenceManager.getDefault().saveContents(view.getController().getContents());
        
    }
    
    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
    
    public String getLayerName() {
        return layerName;
    }
    
    /** Check if in drawing */
    public boolean isInDrawing() {
        HandledChart handledChart = getSelectedHandledChart();
        
        return handledChart != null && !handledChart.isAccomplished() ? true : false;
    }
    
    @Override
    protected void plotPane() {
        /** re-plot handles for selectedHandledChart */
        if (selectedHandledChart != null) {
            if (selectedHandledChart.isActivated() && selectedHandledChart.isAccomplished()) {
                final PathWidget pathWidget = addWidget(new PathWidget());
                pathWidget.setForeground(LookFeel.getCurrent().handleColor);
                pathWidget.getPath().append(selectedHandledChart.getAllCurrentHandlesPath(), false);
            }
        }
    }
    
    public void activate() {
        if (!givenHandledChartsLoaded) {
            /** lazy load */
            loadGivenHandledCharts();
        }
        setVisible(true);
        this.activated = true;
    }
    
    public void passivate() {
        setVisible(false);
        this.activated = false;
    }
    
    public boolean isActivated() {
        return activated;
    }
    
    @Override
    protected void finalize() throws Throwable {
        /**
         * todo
         * remove all mouse listerners that add by HandledChart
         */
        
        super.finalize();
    }
    
}

