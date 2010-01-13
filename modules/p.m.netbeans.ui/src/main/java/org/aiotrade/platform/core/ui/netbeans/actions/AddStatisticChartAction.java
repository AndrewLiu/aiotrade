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

import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.chart.Chart;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.platform.core.analysis.indicator.ProbMassIndicator;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
public class AddStatisticChartAction extends CallableSystemAction {
    
    AddStatisticChartAction() {
    }
    
    public void performAction() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
                    
                    if (analysisWin == null) {
                        return;
                    }
                    
                    final ChartViewContainer viewContainer = analysisWin.getSelectedViewContainer();
                    Chart<?> selectedChart = viewContainer.getSelectedChart();
                    if (selectedChart == null) {
                        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Please select a chart first, by Ctrl + clicking on it!");
                        return;
                    }
                    
                    ChartView selectedView = null;
                    Var<?> selectedVar = null;
                    /** search in masterView's overlappingCharts first */
                    for (Ser ser : viewContainer.getMasterView().getOverlappingSers()) {
                        Map<Chart<?>, Set<Var<?>>> chartMapVars = viewContainer.getMasterView().getChartMapVars(ser);
                        for (Chart<?> chart : chartMapVars.keySet()) {
                            if (chart == selectedChart) {
                                Set<Var<?>> vars = chartMapVars.get(chart);
                                if (vars != null) {
                                    for (Var<?> var : vars) {
                                        /** simply pick up first var? */
                                        selectedVar = var;
                                        selectedView = viewContainer.getMasterView();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (selectedVar == null) {
                        /** then search in all slaveView's mainSerCharts */
                        for (ChartView view : viewContainer.getSlaveViews()) {
                            Map<Chart<?>, Set<Var<?>>> chartMapVars = view.getMainSerChartMapVars();
                            for (Chart<?> chart : chartMapVars.keySet()) {
                                if (chart == selectedChart) {
                                    Set<Var<?>> vars = chartMapVars.get(chart);
                                    if (vars != null) {
                                        for (Var<?> var : vars) {
                                            /** simply pick up first var? */
                                            selectedVar = var;
                                            selectedView = view;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (selectedVar == null) {
                        return;
                    }
                    
                    final Ser baseSer = analysisWin.getSelectedViewContainer().getController().getMasterSer();
                    ProbMassIndicator statIndicator = new ProbMassIndicator(baseSer);
                    statIndicator.setAppliedVar(selectedVar);
                    statIndicator.computeFrom(0);
                    
                    selectedView.addOverlappingCharts(statIndicator);
                    
                    selectedView.repaint();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    public String getName() {
        return "Add Statistic Chart";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/addStatChart.png";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}
