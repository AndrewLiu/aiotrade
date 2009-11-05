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
package org.aiotrade.lib.charting.view.pane

import java.awt.BorderLayout
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartView

/**
 *
 * @author Caoyuan Deng
 */
class YControlPane(view: ChartView, datumPlane: DatumPlane) extends Pane(view, datumPlane) {

  setOpaque(false)
  setRenderStrategy(RenderStrategy.NoneBuffer)
        
  val scrollControl = new MyScrollControl
  scrollControl.setExtendable(false)
  scrollControl.setScalable(false)

  setLayout(new BorderLayout());
  add(scrollControl, BorderLayout.CENTER)
    
  def setAutoHidden(b: Boolean) {
    scrollControl.setAutoHidden(b)
  }
    
  def syncWithView {
    val mainChartPane = view.getMainChartPane
        
    val yChartScale = mainChartPane.getYChartScale
        
    val vModelRange = 1.0
    val modelEnd = 1.0
    val vShownRange = 0.2
    val vShownEnd = yChartScale
        
    val unit = 0.05f
    val nUnitsBlock = 3
        
    scrollControl.setValues(vModelRange, vShownRange, modelEnd, vShownEnd, unit, nUnitsBlock)
        
    val autoHidden = LookFeel().isAutoHideScroll
    scrollControl.setAutoHidden(autoHidden)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
  class MyScrollControl extends AbstractScrollControl {
    protected def viewScrolledByUnit(nUnitsWithDirection: Double) {
      val yChartScale = scrollControl.getValueShownEnd.asInstanceOf[Float]
            
      view.setYChartScale(yChartScale)
    }
        
    protected def viewScaledToRange(viewRange: Double) {
    }
  }
    
  @deprecated
  def syncWithView_scrollChart {
    val mainChartPane = view.getMainChartPane
        
    val hCanvas = mainChartPane.getHCanvas
    val yCanvasCenter = mainChartPane.getYCanvasUpper + hCanvas * 0.5
        
    /** define the modelRange, as the value range of chart is relative fixed, so: */
        
    val chartValueBeg = mainChartPane.getMinValue
    val chartValueEnd = mainChartPane.getMaxValue
    val chartValueRange = chartValueEnd - chartValueBeg
    /** give 8 times space for scrolling */
    val modelValueRange = chartValueRange * 8.0
    val modelRange = modelValueRange
        
    /** now try to find the modelBeg and modelEnd, we can decide the middle is at canvas center: */
    val modelCenter = mainChartPane.vy(yCanvasCenter.floatValue)
    val modelEnd = modelCenter + modelRange * 0.5
    val modelBeg = modelEnd - modelRange
        
    val canvasValueBeg = mainChartPane.vy(mainChartPane.getYChartLower)
    val canvasValueEnd = mainChartPane.vy(mainChartPane.getYChartUpper)
    val canvasValueRange = canvasValueEnd - canvasValueBeg
        
    val viewRange = canvasValueRange
    val viewEnd = canvasValueEnd
        
    /** the unit here is value-per-pixels, so when 1 UNIT is moved, will scroll unit value on pane */
    val unit = 1.0 / mainChartPane.getHOne
    val blockUnits = (hCanvas * 0.168 / mainChartPane.getHOne).intValue
        
    scrollControl.setValues(modelRange, viewRange, modelEnd, viewEnd, unit, blockUnits)
  }
    
  @deprecated private class MyScrollControl_scrollChart extends AbstractScrollControl {
    val mainChartPane = view.getMainChartPane
        
    protected def viewScrolledByUnit(nUnitsWithDirection: Double) {
      view.scrollChartsVerticallyByPixel(nUnitsWithDirection.intValue)
    }
        
    protected def viewScaledToRange(viewRange: Double) {
      view.setYChartScaleByCanvasValueRange(viewRange)
    }
  }
    
}


