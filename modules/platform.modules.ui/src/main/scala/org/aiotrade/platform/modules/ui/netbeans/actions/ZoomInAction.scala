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

import javax.swing.JOptionPane;
import org.aiotrade.platform.modules.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.modules.ui.netbeans.windows.RealTimeBoardTopComponent;
import org.aiotrade.platform.modules.ui.netbeans.windows.RealTimeChartsTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;


/**
 *
 * @author Caoyuan Deng
 */
class ZoomInAction extends CallableSystemAction {
    
  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
          def run() {
            val tc = WindowManager.getDefault().getRegistry().getActivated();
            tc match {
              case x: AnalysisChartTopComponent =>
                x.getSelectedViewContainer.controller.growWBar(+1)
              case x: RealTimeChartsTopComponent =>
                for (c <- x.getViewContainers) {
                  c.controller.growWBar(+1)
                }
              case x: RealTimeBoardTopComponent =>
                x.getChartViewContainer.controller.growWBar(+1)
              case _ =>
                JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Please select a view by clicking on it first!");
            }
          }
        });
    } catch {case ex: Exception =>}
        
  }
    
  def getName: String = {
    return "Zoom In";
  }
    
    
    
    
  def getHelpCtx: HelpCtx = {
    return HelpCtx.DEFAULT_HELP;
  }
    
  override
  protected def iconResource: String = {
    return "org/aiotrade/platform/modules/ui/netbeans/resources/zoomIn.gif";
  }
    
  override
  protected def asynchronous: Boolean = {
    return false;
  }
    
    
}
