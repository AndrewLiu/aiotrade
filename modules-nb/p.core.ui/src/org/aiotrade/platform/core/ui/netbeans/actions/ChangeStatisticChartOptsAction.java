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
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.lib.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.platform.core.analysis.indicator.ProbMassIndicator;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.core.ui.dialog.ChangeIndicatorOptsPane;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
public class ChangeStatisticChartOptsAction extends CallableSystemAction {
    IndicatorDescriptor descriptor;
    Indicator indicator;
    
    ChangeStatisticChartOptsAction() {
    }
    
    public void performAction() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    AnalysisChartTopComponent analysisTc = AnalysisChartTopComponent.getSelected();
                    
                    if (analysisTc == null) {
                        return;
                    }
                    
                    ChartView selectedView = analysisTc.getSelectedViewContainer().getSelectedView();
                    indicator = null;
                    
                    for (Ser ser : selectedView.getOverlappingSers()) {
                        indicator = (Indicator)ser;
                        if (indicator instanceof ProbMassIndicator) {
                            /** only pick first statistic one */
                            break;
                        }
                    }
                    
                    if (indicator == null) {
                        return;
                    }
                    
                    descriptor = new IndicatorDescriptor();
                    descriptor.setServiceClassName(indicator.getClass().getName());
                    descriptor.setOpts(indicator.getOpts());
                    
                    ChangeIndicatorOptsPane pane = new ChangeIndicatorOptsPane(WindowManager.getDefault().getMainWindow(), descriptor);
                    
                    /** added listener, so when spnner changed, could preview */
                    ChangeListener spinnerChangeListener = new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            indicator.setOpts(descriptor.getOpts());
                        }
                    };
                    
                    pane.addSpinnerChangeListener(spinnerChangeListener);
                    
                    int retValue = pane.showDialog();
                    
                    pane.removeSpinnerChangeListener(spinnerChangeListener);
                    
                    if (retValue == JOptionPane.OK_OPTION) {
                        
                        indicator.setOpts(descriptor.getOpts());
                        
                    } else {
                        
                        /** opts may has been changed when preview, so, should do setOpts to restore old params to indicator instance */
                        indicator.setOpts(descriptor.getOpts());
                        
                    }
                }
                
            });
        } catch (Exception e) {
        }
        
    }
    
    public String getName() {
        return "Change Statistic Chart's Options";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
}

