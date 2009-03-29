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
import org.aiotrade.util.swing.action.EditAction;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.WithQuoteChart;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class ChangeOptsAction extends CallableSystemAction {
    
    /** Creates a new instance
     */
    public ChangeOptsAction() {
    }
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
                if (analysisWin == null) {
                    return;
                }
                
                ChartView selectedView = analysisWin.getSelectedViewContainer().getSelectedView();
                if (selectedView == null) {
                    return;
                }
                
                Indicator indicator = null;
                if (selectedView instanceof WithQuoteChart) {
                    for (Ser series : selectedView.getOverlappingSers()) {
                        if (series instanceof Indicator) {
                            indicator = (Indicator)series;
                            /** only pick first one */
                            break;
                        }
                    }
                } else {
                    Ser ser = selectedView.getMainSer();
                    if (ser != null && ser instanceof Indicator) {
                        indicator = (Indicator)selectedView.getMainSer();
                    }
                }
                if (indicator == null) {
                    return;
                }
                
                AnalysisContents contents = analysisWin.getSelectedViewContainer().getController().getContents();
                IndicatorDescriptor descriptor = contents.lookupDescriptor(
                        IndicatorDescriptor.class,
                        indicator.getClass().getName(),
                        selectedView.getMainSer().getFreq());
                if (descriptor == null) {
                    return;
                }
                
                descriptor.lookupAction(EditAction.class).execute();
            }
        });
        
    }
    
    public String getName() {
        return "Change Current Indicator's Options";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/changeOpts.gif";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    
}


