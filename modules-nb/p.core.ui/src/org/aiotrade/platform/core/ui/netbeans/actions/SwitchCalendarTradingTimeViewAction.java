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
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.core.ui.netbeans.windows.RealtimeChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 *
 * @author Caoyuan Deng
 */
public class SwitchCalendarTradingTimeViewAction extends CallableSystemAction {
    
    private static JToggleButton toggleButton;
    
    public SwitchCalendarTradingTimeViewAction() {
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
        return "Calendar/Trading date View";
    }
    
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/naturalTrading.gif";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    public static void updateToolbar(ChartViewContainer selectedViewContainer) {
        boolean selected = selectedViewContainer.getController().isOnCalendarMode();
        toggleButton.setSelected(selected);
    }
    
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/naturalTrading.gif");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Calendar/Trading date View");
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int state = e.getStateChange();
                
                TopComponent tc = WindowManager.getDefault().getRegistry().getActivated();
                if (tc == null) return;
                
                ChartViewContainer viewContainer = null;
                if (tc instanceof AnalysisChartTopComponent) {
                    viewContainer = ((AnalysisChartTopComponent)tc).getSelectedViewContainer();
                } else if (tc instanceof RealtimeChartTopComponent) {
                    viewContainer = ((RealtimeChartTopComponent)tc).getViewContainer();
                } else {
                    JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Please select a view by clicking on it first!");
                }
                
                if (state == ItemEvent.SELECTED) {
                    if (viewContainer != null && !viewContainer.getController().isOnCalendarMode()) {
                        viewContainer.getController().setOnCalendarMode(true);
                    }
                } else {
                    if (viewContainer != null && viewContainer.getController().isOnCalendarMode()) {
                        viewContainer.getController().setOnCalendarMode(false);
                    }
                }
            }
        });
        
        return toggleButton;
    }
    
}

