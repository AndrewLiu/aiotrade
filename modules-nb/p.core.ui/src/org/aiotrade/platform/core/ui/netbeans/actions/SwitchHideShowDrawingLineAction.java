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
package org.aiotrade.platform.core.ui.netbeans.actions;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import org.aiotrade.util.swing.action.HideAction;
import org.aiotrade.util.swing.action.ViewAction;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.math.timeseries.MasterSer;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class SwitchHideShowDrawingLineAction extends CallableSystemAction {
    private static JToggleButton toggleButton;
    
    /** Creates a new instance
     */
    public SwitchHideShowDrawingLineAction() {
    }
    
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (toggleButton.isSelected()) {
                    toggleButton.setSelected(false);
                } else {
                    toggleButton.setSelected(true);
                }
            }
        });
        
    }
    
    public String getName() {
        return "Hide or Show Drawing Line";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/hideDrawingLine.gif";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    public static void updateToolbar(ChartViewContainer selectedViewContainer) {
        ChartView masterView = selectedViewContainer.getMasterView();
        if (masterView instanceof WithDrawingPane) {
            DrawingPane drawing = ((WithDrawingPane)masterView).getSelectedDrawing();
            if (drawing != null) {
                boolean selected = drawing.isActivated();
                toggleButton.setSelected(selected);
            } else {
                toggleButton.setSelected(false);
            }
        }
    }
    
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/hideDrawingLine.gif");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Show Drawing");
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
                if (analysisWin == null) {
                    return;
                }
                
                ChartViewContainer viewContainer = analysisWin.getSelectedViewContainer();
                WithDrawingPane masterView = (WithDrawingPane)viewContainer.getMasterView();
                if (masterView.getSelectedDrawing() == null) {
                    return;
                }
                
                MasterSer masterSer = viewContainer.getController().getMasterSer();
                AnalysisContents contents = viewContainer.getController().getContents();
                DrawingDescriptor descriptor = contents.lookupDescriptor(
                        DrawingDescriptor.class,
                        masterView.getSelectedDrawing().getLayerName(),
                        masterSer.getFreq());
                if (descriptor == null) {
                    return;
                }
                
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    /** judge again to aviod recursively calling */
                    if (!((WithDrawingPane)masterView).getSelectedDrawing().isActivated()) {
                        descriptor.lookupAction(ViewAction.class).execute();
                    }
                } else {
                    if (((WithDrawingPane)masterView).getSelectedDrawing().isActivated()) {
                        descriptor.lookupAction(HideAction.class).execute();
                    }
                }
            }
        });
        
        return toggleButton;
    }
    
}



