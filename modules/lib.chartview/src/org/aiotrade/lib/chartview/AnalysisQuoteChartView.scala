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
package org.aiotrade.lib.chartview

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.WithDrawingPaneHelper
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.timeseries.computable.DefaultFactor
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.view.pane.DrawingPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.charting.view.pane.XControlPane
import org.aiotrade.lib.charting.view.pane.YControlPane
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.collection.ArrayList
import scala.collection.mutable.HashMap


/**
 *
 * @author Caoyuan Deng
 */
object AnalysisQuoteChartView {
  /** all AnalysisQuoteChartView instances share the same type */
  private var quoteChartType: QuoteChart.Type = null

  def switchAllQuoteChartType(tpe: QuoteChart.Type) {
    quoteChartType = AbstractQuoteChartView.internal_switchAllQuoteChartType(quoteChartType, tpe)
  }
}
class AnalysisQuoteChartView(controller: ChartingController,
                             quoteSer: QuoteSer,
                             empty: Boolean
) extends {
  private var compareIndicatorToChart: HashMap[QuoteCompareIndicator, QuoteChart] = _
  private var withDrawingPaneHelper: WithDrawingPaneHelper = _
} with AbstractQuoteChartView(controller, quoteSer, empty) with WithDrawingPane {
  import AnalysisQuoteChartView._
    
  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)
    
  override def init(controller: ChartingController, quoteSer: TSer) {
    quoteChartType = LookFeel().getQuoteChartType
        
    compareIndicatorToChart = new HashMap

    /**
     * To avoid null withDrawingPaneHelper when getSelectedDrawing called by other
     * threads (such as dataLoadServer is running and fire a SerChangeEvent
     * to force a updateView() calling), we should create withDrawingPaneHelper before super.init call
     * (this will makes it be called before the code:
     *     this.mainSer.addSerChangeListener(serChangeListener)
     * in it's super's constructor: @See:ChartView#ChartView(ChartViewContainer, Ser)
     */
    withDrawingPaneHelper = new WithDrawingPaneHelper(this)

    super.init(controller, quoteSer)
  }
    
  protected def initComponents {
    xControlPane = new XControlPane(this, mainChartPane)
    xControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    yControlPane = new YControlPane(this, mainChartPane)
    yControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    /** begin to set the layout: */
        
    setLayout(new GridBagLayout)
    val gbc = new GridBagConstraints
        
    /**
     * !NOTICE be ware of the components added order:
     * 1. add xControlPane, it will partly cover glassPane in SOUTH,
     * 2. add glassPane, it will exactly cover mainLayeredPane
     * 3. add mainLayeredPane.
     *
     * After that, xControlPane can accept its self mouse events, and so do
     * glassPane except the SOUTH part covered by xControlPane.
     *
     * And glassPane will forward mouse events to whom it covered.
     * @see GlassPane#processMouseEvent(MouseEvent) and
     *      GlassPane#processMouseMotionEvent(MouseEvent)
     */
        
    gbc.anchor = GridBagConstraints.SOUTH
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(xControlPane, gbc)
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 100 - 100 / 6.18
    add(glassPane, gbc)
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 100 - 100 / 6.18
    add(mainLayeredPane, gbc)
        
    /** add the yControlPane first, it will cover axisYPane partly in SOUTH */
    gbc.anchor = GridBagConstraints.SOUTH
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 0
    gbc.weighty = 0
    add(yControlPane, gbc)
        
    /**
     * add the axisYPane in the same grid as yControlPane then, it will be
     * covered by yControlPane partly in SOUTH
     */
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 0
    gbc.weighty = 100
    add(axisYPane, gbc)
        
    /** add axisXPane and dividentPane across 2 gridwidth horizontally, */
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 2
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(axisXPane, gbc)
        
    gbc.anchor = GridBagConstraints.SOUTH
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 2
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(divisionPane, gbc)
  }
    
  override protected def computeGeometry {
    super.computeGeometry
        
    if (getCompareIndicators.size > 0) {
      refreshQuoteCompareSer
      calcMaxMinWithComparingQuotes
    }
  }
    
  def getQuoteChartType: QuoteChart.Type = {
    quoteChartType
  }
    
  def switchQuoteChartType(tpe: QuoteChart.Type) {
    switchAllQuoteChartType(tpe)
        
    repaint()
  }
    
  private def refreshQuoteCompareSer {
    val optsForCompareIndicator = new ArrayList[Factor]
        
    optsForCompareIndicator += (new DefaultFactor("Begin of Time Frame", rb(1)))
    optsForCompareIndicator += (new DefaultFactor("End of Time Frame",   rb(getNBars)))
    optsForCompareIndicator += (new DefaultFactor("Max Value", getMaxValue))
    optsForCompareIndicator += (new DefaultFactor("Min Value", getMinValue))
        
    for (ser <- getCompareIndicators) {
      ser.factors = optsForCompareIndicator
    }
  }
    
  /** calculate maxValue and minValue again, including comparing quotes */
  private def calcMaxMinWithComparingQuotes {
    var maxValue1 = getMaxValue
    var minValue1 = getMinValue
    for (ser <- getCompareIndicators) {
      var i = 1
      while (i <= getNBars) {
        val time = tb(i)
        val item = ser.itemOf(time)
        if (item != null) {
          val compareHi = item.getFloat(ser.high)
          val compareLo = item.getFloat(ser.low)
          if (Null.not(compareHi) && Null.not(compareLo) && compareHi * compareLo != 0 ) {
            maxValue1 = Math.max(maxValue1, compareHi)
            minValue1 = Math.min(minValue1, compareLo)
          }
        }

        i += 1
      }
    }
        
        
    if (maxValue1 == minValue1) {
      maxValue1 += 1
    }
        
    setMaxMinValue(maxValue1, minValue1)
  }
    
  def getCompareIndicators = {
    compareIndicatorToChart.keySet
  }
    
  def getCompareIndicatorMapChart: HashMap[QuoteCompareIndicator, QuoteChart] = {
    compareIndicatorToChart
  }
    
  def addQuoteCompareChart(ser: QuoteCompareIndicator) {
    ser.addSerChangeListener(serChangeListener)
        
    val chart = new QuoteChart
    compareIndicatorToChart.put(ser, chart)
        
    val depth = Pane.DEPTH_CHART_BEGIN + compareIndicatorToChart.size
        
    chart.model.set(
      ser.open,
      ser.high,
      ser.low,
      ser.close)
        
    chart.set(mainChartPane, ser, depth)
    mainChartPane.putChart(chart)
        
    repaint()
  }
    
  def removeQuoteCompareChart(ser: QuoteCompareIndicator) {
    ser.removeSerChangeListener(serChangeListener);
        
    compareIndicatorToChart.get(ser) foreach {chart =>
      mainChartPane.removeChart(chart)
      compareIndicatorToChart.remove(ser)
            
      repaint()
    }
  }
    
  /**
   * implement of WithDrawingPane
   * -------------------------------------------------------
   */
    
  def getSelectedDrawing: DrawingPane = {
    withDrawingPaneHelper.getSelectedDrawing
  }
    
  def setSelectedDrawing(drawing: DrawingPane) {
    withDrawingPaneHelper.setSelectedDrawing(drawing)
  }
    
  def addDrawing(descriptor: DrawingDescriptor, drawing: DrawingPane) {
    withDrawingPaneHelper.addDrawing(descriptor, drawing)
  }
    
  def deleteDrawing(descriptor: DrawingDescriptor) {
    withDrawingPaneHelper.deleteDrawing(descriptor)
  }
    
  def getDescriptorMapDrawing: HashMap[DrawingDescriptor, DrawingPane] = {
    withDrawingPaneHelper.getDescriptorMapDrawing
  }

  @ throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
}

