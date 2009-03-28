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
package org.aiotrade.charting.descriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.charting.chart.handledchart.HandledChart;
import org.aiotrade.charting.chart.util.ValuePoint;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.pane.DrawingPane;

/**
 *
 * @author Caoyuan Deng
 */
public class DrawingDescriptor extends AnalysisDescriptor<DrawingPane> {
    private Map<HandledChart, List<ValuePoint>> handledChartMapPoints = 
            new HashMap<HandledChart, List<ValuePoint>>();

    private String displayName = "Layout One";
    
    public DrawingDescriptor() {
    }
    
    @Override
    public void set(String layerName, Frequency freq) {
        this.displayName = layerName;
        setFreq(freq);
    }
    
    @Override
    public void setServiceClassName(String layerName) {
        setDisplayName(layerName);
    }
    
    @Override
    public String getServiceClassName() {
        return getDisplayName();
    }
    
    public DrawingDescriptor(String layerName) {
        setServiceClassName(layerName);
        setDisplayName(layerName);
    }
    
    public void putHandledChart(HandledChart handledChart, List<ValuePoint> handlePoints) {
        handledChartMapPoints.put(handledChart, handlePoints);
    }
    
    public void removeHandledChart(HandledChart handledChart) {
        handledChartMapPoints.remove(handledChart);
    }
    
    public Map<HandledChart, List<ValuePoint>> getHandledChartMapPoints() {
        return handledChartMapPoints;
    }
    
    public void setHandledChartMapPoints(Map<HandledChart, List<ValuePoint>> handledChartMapPoints) {
        this.handledChartMapPoints = handledChartMapPoints;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @param a Chartview on which the drawing pane is going to stand.
     */
    protected DrawingPane createServiceInstance(Object... args) {
        final ChartView view = (ChartView)args[0];
        return new DrawingPane(view, view.getMainChartPane(), this);
    }
    
    @Override
    public Action[] createDefaultActions() {
        return DrawingDescriptorActionFactory.getDefault().createActions(this);
    }
    
}


