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
package org.aiotrade.platform.modules.ui.netbeans.actions;

import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.chart.Chart;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.indicator.ProbMassIndicator
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.platform.modules.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
class AddStatisticChartAction extends CallableSystemAction {
    
  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable {
          def run {
            val analysisWin = AnalysisChartTopComponent.getSelected
                    
            if (analysisWin == null) {
              return
            }
                    
            val viewContainer = analysisWin.getSelectedViewContainer
            val selectedChart = viewContainer.selectedChart
            if (selectedChart == null) {
              JOptionPane.showMessageDialog(WindowManager.getDefault.getMainWindow, "Please select a chart first, by Ctrl + clicking on it!");
              return
            }
                    
            var selectedView: ChartView = null
            var selectedVar: TVar[_] = null
            /** search in masterView's overlappingCharts first */
            for (ser <- viewContainer.masterView.overlappingSers) {
              val chartToVars = viewContainer.masterView.chartMapVars(ser)
              for ((chart, vars) <- chartToVars if chart eq selectedChart) {
                /** simply pick up first var? */
                vars.find(x => true) foreach {v =>
                  selectedVar = v
                  selectedView = viewContainer.masterView
                }
              }
            }

            if (selectedVar == null) {
              /** then search in all slaveView's mainSerCharts */
              for (view <- viewContainer.slaveViews) {
                val chartToVars = view.mainSerChartToVars
                //chartToVars.get(selectedChart)
                for ((chart, vars) <- chartToVars if chart eq selectedChart) {
                  /** simply pick up first var? */
                  vars.find(x => true) foreach {v =>
                    selectedVar = v
                    selectedView = view
                  }
                }
              }
            }
                    
            if (selectedVar == null) {
              return
            }
                    
            val baseSer = analysisWin.getSelectedViewContainer.controller.masterSer
            val statIndicator = new ProbMassIndicator(baseSer)
            statIndicator.baseVar = selectedVar.asInstanceOf[TVar[Float]]
            statIndicator.computeFrom(0)
                    
            selectedView.addOverlappingCharts(statIndicator)
                    
            selectedView.repaint()
          }
        })
    } catch {case ex: Exception => ex.printStackTrace}
        
  }
    
  def getName = {
    "Add Statistic Chart"
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/platform/modules/ui/netbeans/resources/addStatChart.png";
  }
    
  override protected def asynchronous = {
    false
  }
    
}
