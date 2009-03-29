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
import java.lang.ref.WeakReference;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.core.ui.netbeans.windows.RealtimeChartsTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
public class SwitchHideShowCursorCrossAction extends CallableSystemAction {
    
    /** Creates a new instance
     */
    public SwitchHideShowCursorCrossAction() {
    }
    
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                TopComponent tc = WindowManager.getDefault().getRegistry().getActivated();
                if (tc == null) {
                    return;
                }
                
                ChartingController controller = null;
                if (tc instanceof AnalysisChartTopComponent) {
                    for (WeakReference<AnalysisChartTopComponent> ref : AnalysisChartTopComponent.getInstanceRefs()) {
                        controller = ref.get().getSelectedViewContainer().getController();
                        controller.setCursorCrossLineVisible(!controller.isCursorCrossLineVisible());
                        controller.updateViews();
                    }
                } else if (tc instanceof RealtimeChartsTopComponent) {
                    for (WeakReference<RealtimeChartsTopComponent> ref : RealtimeChartsTopComponent.getInstanceRefs()) {
                        for (ChartViewContainer vc : ref.get().getViewContainers()) {
                            controller = vc.getController();
                            controller.setCursorCrossLineVisible(!controller.isCursorCrossLineVisible());
                            controller.updateViews();
                        }
                    }
                }
            }
        });
        
    }
    
    public String getName() {
        return "Hide/Show Cross Cursor";
    }
    
    
    
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/hideCursorCross.gif";
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    
}




