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
package org.aiotrade.platform.core.ui.netbeans.actionfactories;

import java.util.Collection;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptorActionFactory;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.WithDrawingPane;
import org.aiotrade.lib.charting.view.pane.DrawingPane;
import org.aiotrade.lib.util.swing.action.DeleteAction;
import org.aiotrade.lib.util.swing.action.HideAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 11, 2006, 10:20 PM
 * @since   1.0.4
 */
public class NetBeansDrawingDescriptorActionFactory implements DrawingDescriptorActionFactory.I {
    
    public Action[] createActions(DrawingDescriptor descriptor) {
        return new Action[] {
            new DrawingViewAction(descriptor),
            new DrawingHideAction(descriptor),
            new DrawingDeleteAction(descriptor)
        };
    }
    
    
    private static class DrawingViewAction extends ViewAction {
        private final DrawingDescriptor descriptor;
        
        DrawingViewAction(DrawingDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Show");
        }
        
        public void execute() {
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                    descriptor.getContainerContents().getUniSymbol());
            if (analysisWin != null) {
                ChartViewContainer viewContainer = analysisWin.lookupViewContainer(descriptor.getFreq());
                if (viewContainer != null) {
                    descriptor.setActive(true);
                    
                    ChartView masterView = viewContainer.getMasterView();
                    WithDrawingPane withDrawingPane = (WithDrawingPane)masterView;
                    DrawingPane drawing = withDrawingPane.getDescriptorMapDrawing().get(descriptor);
                    if (drawing != null) {
                        withDrawingPane.setSelectedDrawing(drawing);
                    } else {
                        drawing = descriptor.getServiceInstance(masterView);
                        withDrawingPane.addDrawing(descriptor, drawing);
                    }
                    
                    viewContainer.getController().setCursorCrossLineVisible(false);
                    drawing.activate();
                    
                    /** hide other drawings */
                    final Collection<DrawingDescriptor> descriptors = descriptor.getContainerContents().lookupDescriptors(
                            DrawingDescriptor.class, 
                            descriptor.getFreq());
                    for (DrawingDescriptor _descriptor : descriptors) {
                        if (_descriptor != descriptor && _descriptor.isActive()) {
                            _descriptor.lookupAction(HideAction.class).execute();
                        }
                        
                    }
                    
                    analysisWin.requestActive();
                    analysisWin.setSelectedViewContainer(viewContainer);
                }
            }
            
        }
        
    }
    
    private class DrawingHideAction extends HideAction {
        private final DrawingDescriptor descriptor;
        
        DrawingHideAction(DrawingDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Hide");
        }
        
        public void execute() {
            descriptor.setActive(false);
            
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                    descriptor.getContainerContents().getUniSymbol());
            if (analysisWin != null) {
                ChartViewContainer viewContainer = analysisWin.lookupViewContainer(descriptor.getFreq());
                if (viewContainer != null) {
                    ChartView masterView = viewContainer.getMasterView();
                    
                    DrawingPane drawing = ((WithDrawingPane)masterView).getDescriptorMapDrawing().get(descriptor);
                    if (drawing != null) {
                        drawing.passivate();
                    }
                    
                    viewContainer.getController().setCursorCrossLineVisible(true);
                    analysisWin.requestActive();
                    analysisWin.setSelectedViewContainer(viewContainer);
                }
            }
        }
        
    }
    
    private static class DrawingDeleteAction extends DeleteAction {
        private final DrawingDescriptor descriptor;
        
        DrawingDeleteAction(DrawingDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Delete");
        }
        
        public void execute() {
            int confirm = JOptionPane.showConfirmDialog(
                    WindowManager.getDefault().getMainWindow(),
                    "Are you sure you want to delete drawing: " + descriptor.getDisplayName() + " ?",
                    "Deleting drawing ...",
                    JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                descriptor.getContainerContents().removeDescriptor(descriptor);
                descriptor.getContainerContents().lookupAction(SaveAction.class).execute();
                
                AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                        descriptor.getContainerContents().getUniSymbol());
                if (analysisWin != null) {
                    ChartView masterView = analysisWin.getSelectedViewContainer().getMasterView();
                    
                    ((WithDrawingPane)masterView).deleteDrawing(descriptor);
                }
            }
        }
        
    }
    
    
}



