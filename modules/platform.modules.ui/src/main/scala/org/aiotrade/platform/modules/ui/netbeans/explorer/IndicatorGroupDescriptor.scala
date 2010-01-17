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
package org.aiotrade.platform.modules.ui.netbeans.explorer;

import java.awt.Image;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.platform.modules.ui.netbeans.GroupDescriptor
import org.aiotrade.platform.modules.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.modules.ui.dialog.PickIndicatorDialog;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;import scala.collection.mutable.HashMap


/**
 *
 *
 *
 * @author Caoyuan Deng
 */
object IndicatorGroupDescriptor {
  val NAME = "Indicators"
  val ICON = Utilities.loadImage("org/aiotrade/platform/modules/ui/netbeans/resources/indicators.gif")
}
class IndicatorGroupDescriptor extends GroupDescriptor[IndicatorDescriptor] {
  import IndicatorGroupDescriptor._

  def getBindClass: Class[IndicatorDescriptor] = {
    classOf[IndicatorDescriptor]
  }
    
  def createActions(contents: AnalysisContents): Array[Action] = {
    Array(new AddIndicatorAction(contents))
  }
    
  def getDisplayName = {
    NAME
  }
    
  def getTooltip = {
    NAME
  }
    
  def getIcon(tpe: Int): Image = {
    ICON
  }
    
  private class AddIndicatorAction(contents: AnalysisContents) extends AddAction {
    putValue(Action.NAME, "Add Indicator");

    def execute {
      val analysisWin = AnalysisChartTopComponent.getSelected
      if (analysisWin == null) {
        return
      }
            
      var nameMapResult = HashMap[String, Object]()
            
      val dialog = new PickIndicatorDialog(
        WindowManager.getDefault.getMainWindow,
        true,
        nameMapResult
      )
      dialog.setVisible(true)
            
      if (nameMapResult.get("Option").asInstanceOf[Int] != JOptionPane.OK_OPTION) {
        return;
      }
            
      val selectedIndicator = nameMapResult.get("selectedIndicator").asInstanceOf[Indicator]
      val multipleEnable    = nameMapResult.get("multipleEnable").asInstanceOf[Boolean]
      val nUnits            = nameMapResult.get("nUnits").asInstanceOf[Int]
      val unit              = nameMapResult.get("unit").asInstanceOf[TUnit]
            
      if (selectedIndicator == null) {
        return
      }
            
      /**
       * setAllowMultipleIndicatorOnQuoteChartView in OptionManager, let
       * DescriptorNode.IndicatorViewAction or anyone to decide how to treat it.
       */
      LookFeel().setAllowMultipleIndicatorOnQuoteChartView(multipleEnable);
            
      val className = selectedIndicator.getClass.getName
            
      (contents.lookupDescriptor(
        classOf[IndicatorDescriptor],
        className,
        new TFreq(unit, nUnits)
      ) match {
        case None => 
          contents.createDescriptor(
            classOf[IndicatorDescriptor],
            className,
            new TFreq(unit, nUnits)
          )
        case some => some
      }) foreach {descriptor =>
        contents.lookupAction(classOf[SaveAction])   foreach {_.execute}
        descriptor.lookupAction(classOf[ViewAction]) foreach {_.execute}
      }
    }
        
  }

}

