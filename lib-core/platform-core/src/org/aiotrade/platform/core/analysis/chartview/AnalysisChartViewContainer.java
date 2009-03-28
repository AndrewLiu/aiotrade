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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.util.swing.GBC;

/**
 *
 * @author Caoyuan Deng
 */
public class AnalysisChartViewContainer extends ChartViewContainer {
    
    @Override
    public void init(Component focusableParent, ChartingController controller) {
        super.init(focusableParent, controller);
    }
    
    protected void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.weightx = 100;
        gbc.weighty = 618;
        
        QuoteSer quoteSer = (QuoteSer)getController().getMasterSer();
        quoteSer.setShortDescription(getController().getContents().getUniSymbol());
        AnalysisQuoteChartView quoteChartView = new AnalysisQuoteChartView(getController(), quoteSer);
        setMasterView(quoteChartView, gbc);
        
        /** use two list to record the active indicators and their order(index) for later showing */
        List<IndicatorDescriptor> indicatorDescriptorsToBeShowing = new ArrayList<IndicatorDescriptor>();
        List<Indicator>  indicatorsToBeShowing = new ArrayList<Indicator>();
        for (IndicatorDescriptor descriptor : getController().getContents().lookupDescriptors(IndicatorDescriptor.class)) {
            if (descriptor.isActive() && descriptor.getFreq().equals(getController().getMasterSer().getFreq())) {
                final Indicator indicator = descriptor.getServiceInstance(getController().getMasterSer());
                if (indicator != null) {
                    /**
                     * @NOTICE
                     * As the quoteSer may has been loaded, there may be no more UpdatedEvent
                     * etc. fired, so, computeFrom(0) first.
                     */
                    indicator.computeFrom(0); // don't remove me
                    
                    if (indicator.isOverlapping()) {
                        addSlaveView(descriptor, indicator, null);
                    } else {
                        /** To get the extract size of slaveViews to be showing, store them first, then add them later */
                        indicatorDescriptorsToBeShowing.add(descriptor);
                        indicatorsToBeShowing.add(indicator);
                    }
                }
            }
        }
        
        /** now add slaveViews, the size has excluded those indicators not showing */
        int size = indicatorDescriptorsToBeShowing.size();
        for (int i = 0; i < size; i++) {
            gbc.weighty = 382f / (float)size;
            addSlaveView(indicatorDescriptorsToBeShowing.get(i), indicatorsToBeShowing.get(i), gbc);
        }
        
        for (DrawingDescriptor descriptor : getController().getContents().lookupDescriptors(DrawingDescriptor.class)) {
            if (descriptor.getFreq().equals(getController().getMasterSer().getFreq())) {
                DrawingPane drawing = descriptor.getServiceInstance(getMasterView());
                if (drawing != null) {
                    ((WithDrawingPane)getMasterView()).addDrawing(descriptor, drawing);
                }
                
            }
        }
    }
    
}