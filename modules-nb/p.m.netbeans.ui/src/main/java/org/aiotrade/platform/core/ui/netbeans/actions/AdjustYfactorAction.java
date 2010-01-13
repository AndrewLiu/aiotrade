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
import java.awt.Dimension;
import java.awt.Font;
import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class AdjustYfactorAction extends CallableSystemAction {
    private static Font lableFont = new Font("Dialog Input", Font.PLAIN, 9);
    
    private static JSlider yslider;
    
    public AdjustYfactorAction() {
    }
    
    /**
     * @NOTICE
     * If override getToolbarPresenter(), you should process action in that
     * component instead of here.
     */
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            }
        });
        
    }
    
    public String getName() {
        return "Adjust Scale Factor";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected String iconResource() {
        return super.iconResource();
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    public Component getToolbarPresenter() {
        
        yslider = new JSlider(JSlider.VERTICAL);
        yslider.setPaintTrack(true);
        
        yslider.setMinimum(10);  // 0.1 multiple
        yslider.setMaximum(100); // 1.0 multiple
        yslider.setValue(100);
        yslider.setToolTipText("Adjust Scale Factor");
        
        yslider.setMajorTickSpacing(90);
        yslider.setPaintTicks(true);
        yslider.setPaintTrack(false);
        
        Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
        JLabel upperLabel = new JLabel(" 1.0");
        JLabel lowerLabel = new JLabel(" 0.1");
        upperLabel.setFont(lableFont);
        lowerLabel.setFont(lableFont);
        table.put(new Integer(10),  lowerLabel);
        table.put(new Integer(100), upperLabel);
        yslider.setLabelTable(table);
        yslider.setPaintLabels(true);
        
        yslider.setPreferredSize(new Dimension(50, 18));
        yslider.setFocusable(false);
        
        yslider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider s = (JSlider)e.getSource();
                if (!s.getValueIsAdjusting()) { //done adjusting
                    float yfactorControl = (float)s.getValue() / 100.0f;
                    adjustYChartPercentInCanvas(yfactorControl);
                }
            }
        });
        
        return yslider;
    }
    
    private void adjustYChartPercentInCanvas(float yChartPercentInCanvas) {
        AnalysisChartTopComponent analysisTc = AnalysisChartTopComponent.getSelected();
        
        if (analysisTc != null) {
            analysisTc.getSelectedViewContainer().getMasterView().setYChartScale(yChartPercentInCanvas);
        }
    }
}



