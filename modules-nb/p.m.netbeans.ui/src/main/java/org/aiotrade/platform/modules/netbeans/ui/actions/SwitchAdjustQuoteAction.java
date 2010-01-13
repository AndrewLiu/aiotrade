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
package org.aiotrade.platform.modules.netbeans.ui.actions;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.WithQuoteChart;
import org.aiotrade.lib.math.timeseries.QuoteSer;
import org.aiotrade.platform.modules.netbeans.ui.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class SwitchAdjustQuoteAction extends CallableSystemAction {
    private static JToggleButton toggleButton;
    
    public SwitchAdjustQuoteAction() {
    }
    
    
    public void performAction() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (toggleButton.isSelected()) {
                        toggleButton.setSelected(false);
                    } else {
                        toggleButton.setSelected(true);
                    }
                }
            });
        } catch (Exception e) {
        }
        
    }
    
    public String getName() {
        return "Adjust Quote";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/switchAdjust.png";
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    public static void updateToolbar(ChartViewContainer selectedViewContainer) {
        if (selectedViewContainer.getController().getMasterSer() instanceof QuoteSer) {
            boolean selected = ((QuoteSer)selectedViewContainer.getController().getMasterSer()).isAdjusted();
            toggleButton.setSelected(selected);
        }
    }
    
    @Override
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/switchAdjust.png");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Adjust Quote");
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int state = e.getStateChange();
                
                AnalysisChartTopComponent analysisTc = AnalysisChartTopComponent.getSelected();
                
                if (analysisTc == null) {
                    return;
                }
                
                QuoteSer quoteSeries = ((WithQuoteChart)analysisTc.getSelectedViewContainer().getMasterView()).getQuoteSer();
                
                if (state == ItemEvent.SELECTED) {
                    if (!quoteSeries.isAdjusted()) {
                        quoteSeries.adjust(true);
                    }
                } else {
                    if (quoteSeries.isAdjusted()) {
                        quoteSeries.adjust(false);
                    }
                }
                
            }
        });
        
        return toggleButton;
    }
    
}




