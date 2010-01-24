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
package org.aiotrade.modules.ui.netbeans.actions.factory

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.math.timeseries.computable.ComputeFrom
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptorActionFactory
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.util.swing.action.DeleteAction;
import org.aiotrade.lib.util.swing.action.EditAction;
import org.aiotrade.lib.util.swing.action.HideAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.modules.ui.dialog.ChangeIndicatorOptsPane;
import org.aiotrade.modules.ui.netbeans.nodes.IndicatorGroupDescriptor;
import org.aiotrade.modules.ui.netbeans.windows.ExplorerTopComponent;
import org.aiotrade.modules.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.windows.WindowManager;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 11, 2006, 10:20 PM
 * @since   1.0.4
 */
class NetBeansIndicatorDescriptorActionFactory extends IndicatorDescriptorActionFactory {
    
  def createActions(descriptor: IndicatorDescriptor): Array[Action] = {
    Array(
      new IndicatorViewAction(descriptor),
      new IndicatorHideAction(descriptor),
      new IndicatorDeleteAction(descriptor),
      new IndicatorEditAction(descriptor)
    )
  }
    
  private class IndicatorViewAction(descriptor: IndicatorDescriptor) extends ViewAction {
    putValue(Action.NAME, "Show")
        
    def execute {
            
      descriptor.active = true;
      descriptor.containerContents.lookupAction(classOf[SaveAction]) foreach {_.execute}
            
      for (analysisWin <- AnalysisChartTopComponent.instanceOf(descriptor.containerContents.uniSymbol);
           viewContainer = analysisWin.viewContainer;
           view <- viewContainer.lookupChartView(descriptor);
           indicator <- descriptor.serviceInstance(viewContainer.controller.masterSer)
      ) {
        /**
         * @NOTICE
         * descriptor's opts may be set by this call
         */
        indicator.computableActor ! ComputeFrom(0)
                    
        if (indicator.isOverlapping) {
          if (!LookFeel().isAllowMultipleIndicatorOnQuoteChartView) {
            /** hide previous overlapping indicator first if there is one */
            viewContainer.lookupIndicatorDescriptor(viewContainer.masterView) foreach {existedOne =>
              existedOne.lookupAction(classOf[HideAction]).get.execute
            }
          }
          viewContainer.addSlaveView(descriptor, indicator, null)
          viewContainer.repaint();
        } else {
          viewContainer.addSlaveView(descriptor, indicator, null)
          viewContainer.adjustViewsHeight(0);
        }

        viewContainer.selectedView = view
        analysisWin.requestActive
      }
      
    }
  }
    
  private class IndicatorHideAction(descriptor: IndicatorDescriptor) extends HideAction {
    putValue(Action.NAME, "Hide")
        
    def execute {
      descriptor.active = false
      descriptor.containerContents.lookupAction(classOf[SaveAction]) foreach {_.execute}
            
      for (analysisWin <- AnalysisChartTopComponent.instanceOf(descriptor.containerContents.uniSymbol);
           viewContainer = analysisWin.viewContainer
      ) {
        viewContainer.removeSlaveView(descriptor)
                
        analysisWin.requestActive
      }
        
    }
  }
    
  private class IndicatorDeleteAction(descriptor: IndicatorDescriptor) extends DeleteAction {
    putValue(Action.NAME, "Delete")
        
    def execute {
      val confirm = JOptionPane.showConfirmDialog(
        WindowManager.getDefault.getMainWindow,
        "Are you sure you want to delete indicator: " + descriptor.displayName + " ?",
        "Deleting indicator ...",
        JOptionPane.YES_NO_OPTION
      )
            
      if (confirm == JOptionPane.YES_OPTION) {
        descriptor.lookupAction(classOf[HideAction]).get.execute;
                
        descriptor.containerContents.removeDescriptor(descriptor);
        descriptor.containerContents.lookupAction(classOf[SaveAction]) foreach {_.execute}
      }
    }
        
  }
    
    
  /** Action to change options */
  private class IndicatorEditAction(descriptor: IndicatorDescriptor) extends EditAction {
            
    putValue(Action.NAME, "Change Options");
        
    def execute {
      val pane = new ChangeIndicatorOptsPane(WindowManager.getDefault.getMainWindow, descriptor)
            
      /** added listener, so when spnner changed, could preview */
      val spinnerChangeListener = new ChangeListener {
        def stateChanged(e: ChangeEvent) {
          showEffect(descriptor)
        }
      }
            
      pane.addSpinnerChangeListener(spinnerChangeListener);
      val retValue = pane.showDialog();
      pane.removeSpinnerChangeListener(spinnerChangeListener);
            
      if (retValue == JOptionPane.OK_OPTION) {
        /** apple to all ? */
        if (pane.isApplyToAll) {
          val root = ExplorerTopComponent().rootNode
          setIndicatorOptsRecursively(root, descriptor)
        } else { /** else, only apply to this one */
          setIndicatorOpts(descriptor, descriptor.factors)
        }
                
        if (pane.isSaveAsDefault()) {
          val defaultContents = PersistenceManager().defaultContents
          defaultContents.lookupDescriptor(
            classOf[IndicatorDescriptor],
            descriptor.serviceClassName,
            descriptor.freq
          ) match {
            case Some(x) =>
              x.factors = descriptor.factors
            case None =>
              val defaultOne = new IndicatorDescriptor(descriptor.serviceClassName, descriptor.freq, descriptor.factors, false)
              defaultContents.addDescriptor(defaultOne)
          }
                    
          PersistenceManager().saveContents(defaultContents)
        }
      }
      /** else, opts may have been changed when preview, so, should do setOpts to restore old opts to indicator instance */
      else {
        setIndicatorOpts(descriptor, descriptor.factors)
      }
            
    }
        
    /**
     * @TODO
     * If node not expanded yet, getChilder() seems return null, because the children will
     * not be created yet.
     */
    private def setIndicatorOptsRecursively(rootNodeToBeSet: Node, descriptorWithOpts: IndicatorDescriptor) {
      /** folder node ? */
      if (rootNodeToBeSet.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- rootNodeToBeSet.getChildren.getNodes) {
          /** do recursive call */
          setIndicatorOptsRecursively(child, descriptorWithOpts)
        }
      } else { /** else, an OneSymbolNode */
        val contents = rootNodeToBeSet.getLookup.lookup(classOf[AnalysisContents])
        val indicatorGroupNode = rootNodeToBeSet.getChildren.findChild(IndicatorGroupDescriptor.NAME);
        if (indicatorGroupNode != null) {
          for (descriptorToBeSet <- contents.lookupDescriptor(classOf[IndicatorDescriptor],
                                                              descriptorWithOpts.serviceClassName,
                                                              descriptorWithOpts.freq);
               child <- indicatorGroupNode.getChildren.getNodes
          ) {
            setIndicatorOpts(descriptorToBeSet, descriptorWithOpts.factors)
          }
        }
      }
    }
        
    private def setIndicatorOpts(descriptorToBeSet: IndicatorDescriptor, factors: Array[Factor]) {
      descriptorToBeSet.factors = factors
      descriptorToBeSet.containerContents.lookupAction(classOf[SaveAction]) foreach {_.execute}
            
      showEffect(descriptorToBeSet)
    }
        
    private def showEffect(descriptorToBeSet: IndicatorDescriptor) {
      for (analysisWin <- AnalysisChartTopComponent.instanceOf(descriptorToBeSet.containerContents.uniSymbol);
           descriptor <- analysisWin.lookupIndicator(descriptor)
      ) {
        descriptor.factors = descriptorToBeSet.factors
      }
    }
  }
}


