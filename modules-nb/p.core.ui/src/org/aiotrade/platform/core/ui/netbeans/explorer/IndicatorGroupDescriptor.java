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
package org.aiotrade.platform.core.ui.netbeans.explorer;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.Unit;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.core.netbeans.GroupDescriptor;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.core.ui.dialog.PickIndicatorDialog;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 *
 *
 * @author Caoyuan Deng
 */

public class IndicatorGroupDescriptor extends GroupDescriptor<IndicatorDescriptor> {
    
    public final static String NAME = "Indicators";
    private final static Image ICON = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/indicators.gif");
    
    public Class<IndicatorDescriptor> getBindClass() {
        return IndicatorDescriptor.class;
    }
    
    public Action[] createActions(AnalysisContents contents) {
        return new Action[] { new AddIndicatorAction(contents) };
    }
    
    public String getDisplayName() {
        return NAME;
    }
    
    public String getTooltip() {
        return NAME;
    }
    
    public Image getIcon(int type) {
            return ICON;
    }
    
    private static class AddIndicatorAction extends AddAction {
        private final AnalysisContents contents;
        
        AddIndicatorAction(AnalysisContents contents) {
            this.contents = contents;
            
            putValue(Action.NAME, "Add Indicator");
        }
        
        public void execute() {
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.getSelected();
            if (analysisWin == null) {
                return;
            }
            
            Map<String, Object> nameMapResult = new HashMap<String, Object>();
            
            PickIndicatorDialog dialog = new PickIndicatorDialog(
                    WindowManager.getDefault().getMainWindow(),
                    true,
                    nameMapResult);
            dialog.setVisible(true);
            
            if ((Integer)nameMapResult.get("Option") != JOptionPane.OK_OPTION) {
                return;
            }
            
            Indicator selectedIndicator = (Indicator) nameMapResult.get("selectedIndicator");
            Boolean multipleEnable      = (Boolean)   nameMapResult.get("multipleEnable");
            int nUnits                  = (Integer)   nameMapResult.get("nUnits");
            Unit unit                   = (Unit)      nameMapResult.get("unit");
            
            if (selectedIndicator == null) {
                return;
            }
            
            /**
             * setAllowMultipleIndicatorOnQuoteChartView in OptionManager, let
             * DescriptorNode.IndicatorViewAction or anyone to decide how to treat it.
             */
            LookFeel.getCurrent().setAllowMultipleIndicatorOnQuoteChartView(multipleEnable);
            
            String className = selectedIndicator.getClass().getName();
            
            IndicatorDescriptor descriptor = contents.lookupDescriptor(
                    IndicatorDescriptor.class,
                    className,
                    new Frequency(unit, nUnits));
            
            if (descriptor == null) {
                descriptor = contents.createDescriptor(
                        IndicatorDescriptor.class,
                        className,
                        new Frequency(unit, nUnits));
                
            }
            
            contents.lookupAction(SaveAction.class).execute();
            
            descriptor.lookupAction(ViewAction.class).execute();
        }
        
    }

}

