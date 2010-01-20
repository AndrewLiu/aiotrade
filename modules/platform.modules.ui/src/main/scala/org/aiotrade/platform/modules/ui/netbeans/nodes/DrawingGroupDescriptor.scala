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
package org.aiotrade.platform.modules.ui.netbeans.nodes;

import java.awt.Image;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.modules.ui.netbeans.GroupDescriptor
import org.aiotrade.platform.modules.ui.netbeans.windows.AnalysisChartTopComponent;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
object DrawingGroupDescriptor {
  val NAME = "Drawings";
  private val ICON = Utilities.loadImage("org/aiotrade/platform/modules/ui/netbeans/resources/drawings.gif");
}

class DrawingGroupDescriptor extends GroupDescriptor[DrawingDescriptor] {
  import DrawingGroupDescriptor._

  def getBindClass: Class[DrawingDescriptor] = {
    classOf[DrawingDescriptor]
  }
    
  def createActions(contents: AnalysisContents): Array[Action] = {
    Array(new AddDrawingAction(contents))
  }
    
  def getDisplayName: String = {
    NAME
  }
    
  def getTooltip: String = {
    NAME
  }
    
  def getIcon(tpe: Int): Image = {
    ICON
  }
    
  private class AddDrawingAction(contents: AnalysisContents) extends AddAction {
    putValue(Action.NAME, "Add Layer")
        
    def execute {
            
      var layerName = JOptionPane.showInputDialog(
        WindowManager.getDefault.getMainWindow,
        "Please Input Layer Name:",
        "Add Drawing Layer",
        JOptionPane.OK_CANCEL_OPTION
      )
            
      if (layerName == null) {
        return
      }
            
      layerName = layerName.trim
            
      var freq = new TFreq(TUnit.Day, 1)
      AnalysisChartTopComponent.instanceOf(contents.uniSymbol) foreach {analysisTc =>
        analysisTc.selectedViewContainer foreach {viewContainer =>
          freq = viewContainer.controller.masterSer.freq
        }
      }
            
      val descriptor = contents.lookupDescriptor(
        classOf[DrawingDescriptor],
        layerName,
        freq
      ) getOrElse (contents.createDescriptor(classOf[DrawingDescriptor], layerName, freq) getOrElse null)
            
      if (descriptor != null) {
        contents.lookupAction(classOf[SaveAction]) foreach {_.execute}
                
        descriptor.lookupAction(classOf[ViewAction]) foreach {_.execute}
      }
    }
        
  }
    
}

