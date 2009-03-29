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
package org.aiotrade.platform.core.ui.netbeans.explorer;

import java.awt.Image;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.Unit;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.core.netbeans.GroupDescriptor;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */

public class DrawingGroupDescriptor extends GroupDescriptor<DrawingDescriptor> {
    
    public final static String NAME = "Drawings";
    private final static Image ICON = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/drawings.gif");
    
    public Class<DrawingDescriptor> getBindClass() {
        return DrawingDescriptor.class;
    }
    
    public Action[] createActions(AnalysisContents contents) {
        return new Action[] { new AddDrawingAction(contents) };
    }
    
    public String getDisplayName() {
        return NAME;
    }
    
    public String getTooltip()    {
        return NAME;
    }
    
    public Image getIcon(int type) {
        return ICON;
    }
    
    private static class AddDrawingAction extends AddAction {
        private final AnalysisContents contents;
        
        AddDrawingAction(AnalysisContents contents) {
            this.contents = contents;
            
            putValue(Action.NAME, "Add Layer");
        }
        
        public void execute() {
            
            String layerName = JOptionPane.showInputDialog(
                    WindowManager.getDefault().getMainWindow(),
                    "Please Input Layer Name:",
                    "Add Drawing Layer",
                    JOptionPane.OK_CANCEL_OPTION);
            
            if (layerName == null) {
                return;
            }
            
            layerName = layerName.trim();
            
            Frequency freq = new Frequency(Unit.Day, 1);
            AnalysisChartTopComponent analysisTc = AnalysisChartTopComponent.lookupTopComponent(contents.getUniSymbol());
            if (analysisTc != null) {
                ChartViewContainer viewContainer = analysisTc.getSelectedViewContainer();
                if (viewContainer != null) {
                    freq = viewContainer.getController().getMasterSer().getFreq();
                }
            }
            
            DrawingDescriptor descriptor = contents.lookupDescriptor(
                    DrawingDescriptor.class,
                    layerName,
                    freq);
            if (descriptor == null) {
                descriptor = contents.createDescriptor(DrawingDescriptor.class, layerName, freq);
            }
            
            if (descriptor != null) {
                contents.lookupAction(SaveAction.class).execute();
                
                descriptor.lookupAction(ViewAction.class).execute();
            }
        }
        
    }
    
}

