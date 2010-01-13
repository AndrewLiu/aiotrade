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
package org.aiotrade.platform.modules.netbeans.ui.actionfactories;

import java.util.List;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.lib.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.lib.charting.descriptor.IndicatorDescriptorActionFactory;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.DeleteAction;
import org.aiotrade.lib.util.swing.action.EditAction;
import org.aiotrade.lib.util.swing.action.HideAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.modules.ui.dialog.ChangeIndicatorOptsPane;
import org.aiotrade.platform.modules.netbeans.ui.explorer.IndicatorGroupDescriptor;
import org.aiotrade.platform.modules.netbeans.ui.explorer.SymbolListTopComponent;
import org.aiotrade.platform.modules.netbeans.ui.windows.AnalysisChartTopComponent;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.windows.WindowManager;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 11, 2006, 10:20 PM
 * @since   1.0.4
 */
public class NetBeansIndicatorDescriptorActionFactory implements IndicatorDescriptorActionFactory.I {
    
    public Action[] createActions(IndicatorDescriptor descriptor) {
        return new Action[] {
            new IndicatorViewAction(descriptor),
            new IndicatorHideAction(descriptor),
            new IndicatorDeleteAction(descriptor),
            new IndicatorEditAction(descriptor)
        };
    }
    
    private static class IndicatorViewAction extends ViewAction {
        private final IndicatorDescriptor descriptor;
        
        IndicatorViewAction(IndicatorDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Show");
        }
        
        public void execute() {
            
            descriptor.setActive(true);
            descriptor.getContainerContents().lookupAction(SaveAction.class).execute();
            
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                    descriptor.getContainerContents().getUniSymbol());
            if (analysisWin != null) {
                ChartViewContainer viewContainer = analysisWin.lookupViewContainer(descriptor.getFreq());
                ChartView view = viewContainer == null ? null : viewContainer.lookupChartView(descriptor);
                if (view == null) {
                    /**
                     * @NOTICE
                     * descriptor's opts may be set by this call
                     */
                    final Indicator indicator = descriptor.getServiceInstance(viewContainer.getController().getMasterSer());
                    if (indicator == null) {
                        return;
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            indicator.computeFrom(0);
                        }
                    }).start();
                    
                    
                    if (indicator.isOverlapping()) {
                        if (! LookFeel.getCurrent().isAllowMultipleIndicatorOnQuoteChartView()) {
                            /** hide previous overlapping indicator first if there is one */
                            final IndicatorDescriptor existedOne = viewContainer.lookupIndicatorDescriptor(viewContainer.getMasterView());
                            if (existedOne != null) {
                                existedOne.lookupAction(HideAction.class).execute();
                            }
                        }
                        viewContainer.addSlaveView(descriptor, indicator, null);
                        viewContainer.repaint();
                    } else {
                        viewContainer.addSlaveView(descriptor, indicator, null);
                        viewContainer.adjustViewsHeight(0);
                    }
                    
                } else {
                    viewContainer.setSelectedView(view);
                }
                
                analysisWin.requestActive();
                analysisWin.setSelectedViewContainer(viewContainer);
            }
        }
        
    }
    
    private static class IndicatorHideAction extends HideAction {
        private final IndicatorDescriptor descriptor;
        
        IndicatorHideAction(IndicatorDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Hide");
        }
        
        public void execute() {
            descriptor.setActive(false);
            descriptor.getContainerContents().lookupAction(SaveAction.class).execute();
            
            final AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                    descriptor.getContainerContents().getUniSymbol());
            if (analysisWin != null) {
                final ChartViewContainer viewContainer = analysisWin.lookupViewContainer(descriptor.getFreq());
                viewContainer.removeSlaveView(descriptor);
                
                analysisWin.requestActive();
                analysisWin.setSelectedViewContainer(viewContainer);
            }
        }
        
    }
    
    private static class IndicatorDeleteAction extends DeleteAction {
        private final IndicatorDescriptor descriptor;
        
        IndicatorDeleteAction(IndicatorDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Delete");
        }
        
        public void execute() {
            int confirm = JOptionPane.showConfirmDialog(
                    WindowManager.getDefault().getMainWindow(),
                    "Are you sure you want to delete indicator: " + descriptor.getDisplayName() + " ?",
                    "Deleting indicator ...",
                    JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                descriptor.lookupAction(HideAction.class).execute();
                
                descriptor.getContainerContents().removeDescriptor(descriptor);
                descriptor.getContainerContents().lookupAction(SaveAction.class).execute();
            }
        }
        
    }
    
    
    /** Action to change options */
    private static class IndicatorEditAction extends EditAction {
        private final IndicatorDescriptor descriptor;
        
        IndicatorEditAction(IndicatorDescriptor descriptor) {
            this.descriptor = descriptor;
            
            putValue(Action.NAME, "Change Options");
        }
        
        public void execute() {
            ChangeIndicatorOptsPane pane = new ChangeIndicatorOptsPane(WindowManager.getDefault().getMainWindow(), descriptor);
            
            /** added listener, so when spnner changed, could preview */
            ChangeListener spinnerChangeListener = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    showEffect(descriptor);
                }
            };
            
            pane.addSpinnerChangeListener(spinnerChangeListener);
            int retValue = pane.showDialog();
            pane.removeSpinnerChangeListener(spinnerChangeListener);
            
            if (retValue == JOptionPane.OK_OPTION) {
                /** apple to all ? */
                if (pane.isApplyToAll()) {
                    Node root = SymbolListTopComponent.getDefault().getRootNode();
                    setIndicatorOptsRecursively(root, descriptor);
                }
                /** else, only apply to this one */
                else {
                    setIndicatorOpts(descriptor, descriptor.getOpts());
                }
                
                if (pane.isSaveAsDefault()) {
                    AnalysisContents defaultContents = PersistenceManager.getDefault().getDefaultContents();
                    IndicatorDescriptor defaultOne = defaultContents.lookupDescriptor(
                            IndicatorDescriptor.class,
                            descriptor.getServiceClassName(),
                            descriptor.getFreq());
                    
                    if (defaultOne != null) {
                        defaultOne.setOpts(descriptor.getOpts());
                    } else {
                        defaultOne = new IndicatorDescriptor(
                                descriptor.getServiceClassName(), descriptor.getFreq(), descriptor.getOpts(), false);
                        defaultContents.addDescriptor(defaultOne);
                    }
                    
                    PersistenceManager.getDefault().saveContents(defaultContents);
                }
            }
            /** else, opts may have been changed when preview, so, should do setOpts to restore old opts to indicator instance */
            else {
                setIndicatorOpts(descriptor, descriptor.getOpts());
            }
            
        }
        
        /**
         * @TODO
         * If node not expanded yet, getChilder() seems return null, because the children will
         * not be created yet.
         */
        private void setIndicatorOptsRecursively(Node rootNodeToBeSet, IndicatorDescriptor descriptorWithOpts) {
            /** folder node ? */
            if (rootNodeToBeSet.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : rootNodeToBeSet.getChildren().getNodes()) {
                    /** do recursive call */
                    setIndicatorOptsRecursively(child, descriptorWithOpts);
                }
            }
            /** else, an OneSymbolNode */
            else {
                AnalysisContents contents = rootNodeToBeSet.getLookup().lookup(AnalysisContents.class);
                IndicatorDescriptor descriptorToBeSet = contents.lookupDescriptor(
                        IndicatorDescriptor.class,
                        descriptorWithOpts.getServiceClassName(),
                        descriptorWithOpts.getFreq());
                
                Node indicatorGroupNode = rootNodeToBeSet.getChildren().findChild(IndicatorGroupDescriptor.NAME);
                if (indicatorGroupNode != null) {
                    for (Node child : indicatorGroupNode.getChildren().getNodes()) {
                        setIndicatorOpts(descriptorToBeSet, descriptorWithOpts.getOpts());
                    }
                }
            }
        }
        
        private void setIndicatorOpts(IndicatorDescriptor descriptorToBeSet, List<Opt> opts) {
            descriptorToBeSet.setOpts(opts);
            descriptorToBeSet.getContainerContents().lookupAction(SaveAction.class).execute();
            
            showEffect(descriptorToBeSet);
        }
        
        private void showEffect(IndicatorDescriptor descriptorToBeSet) {
            AnalysisChartTopComponent analysisWin = AnalysisChartTopComponent.lookupTopComponent(
                    descriptorToBeSet.getContainerContents().getUniSymbol());
            if (analysisWin != null) {
                Indicator indicator = analysisWin.lookupIndicator(descriptor);
                if (indicator != null) {
                    indicator.setOpts(descriptorToBeSet.getOpts());
                }
            }
        }
    }
    
    
}


