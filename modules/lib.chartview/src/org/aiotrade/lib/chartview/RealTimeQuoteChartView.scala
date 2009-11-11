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
import java.util.Calendar
import java.util.Date
import org.aiotrade.lib.charting.chart.GridChart
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.SerChangeEvent
import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.securities.Market
import org.aiotrade.lib.securities.QuoteItem
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.Ticker

/**
 *
 * @author Caoyuan Deng
 */
object RealTimeQuoteChartView {
  /** all RealtimeQuoteChartView instances share the same type */
  private var quoteChartType: QuoteChart.Type = _
  
  def switchAllQuoteChartType(tpe: QuoteChart.Type) {
    quoteChartType = AbstractQuoteChartView.internal_switchAllQuoteChartType(quoteChartType, tpe)
  }

}

class RealTimeQuoteChartView(acontroller: ChartingController,
                             aquoteSer: QuoteSer,
                             empty: Boolean
) extends {
  private var _prevClose = Null.Float
  private var gridValues: Array[Float] = _
  private var tickerSer: QuoteSer = _
  private val cal = Calendar.getInstance
  private var market: Market = _
} with AbstractQuoteChartView(acontroller, aquoteSer, empty) {
  import RealTimeQuoteChartView._

  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)

  override def init(controller: ChartingController, mainSer: TSer) {
    super.init(controller, mainSer)

    controller.isAutoScrollToNewData = false
    controller.isOnCalendarMode = false
    controller.growWBar(-2)
    axisYPane.isSymmetricOnMiddleValue = true

    RealTimeQuoteChartView.quoteChartType = QuoteChart.Type.Line

    market = sec.market
    tickerSer = sec.tickerSer
    assert(tickerSer != null)
    tickerSer.addSerChangeListener(serChangeListener)
  }

  protected def initComponents {
    glassPane.isUsingInstantTitleValue = true

    setLayout(new GridBagLayout)
    val gbc = new GridBagConstraints

    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 618
    add(glassPane, gbc)

    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 618
    add(mainLayeredPane, gbc)

    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 0
    gbc.weighty = 100
    add(axisYPane, gbc)

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

  override protected def putChartsOfMainSer {
    super.putChartsOfMainSer

    // draw prevClose value grid
    val prevCloseGrid = new GridChart
    prevCloseGrid.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT)
    gridValues = Array(prevClose)
    prevCloseGrid.model.set(gridValues, GridChart.Direction.Horizontal)
    mainChartPane.putChart(prevCloseGrid)
  }

  override def computeMaxMin {
    super.computeMaxMin

    var minValue1 = +Math.MAX_FLOAT
    var maxValue1 = -Math.MAX_FLOAT
    if (Null.not(prevClose)) {
      minValue1 = minValue
      maxValue1 = maxValue
      val maxDelta = Math.max(Math.abs(maxValue1 - prevClose), Math.abs(minValue1 - prevClose))
      maxValue1 = prevClose + maxDelta
      minValue1 = prevClose - maxDelta
      setMaxMinValue(maxValue1, minValue1)
    }

  }

  def quoteChartType: QuoteChart.Type = RealTimeQuoteChartView.quoteChartType
  def switchQuoteChartType(tpe: QuoteChart.Type) {
    switchAllQuoteChartType(tpe)

    repaint()
  }

  def prevClose = _prevClose
  def prevClose_=(prevClose: Float) {
    this._prevClose = prevClose
    gridValues(0) = prevClose
    mainChartPane.referCursorValue = prevClose
    glassPane.referCursorValue = prevClose
  }

  override def popupToDesktop {
    val popupView = new RealTimeQuoteChartView(controller, quoteSer)
    popupView.isInteractive = false
    val dimension = new Dimension(200, 150)
    val alwaysOnTop = true

    controller.popupViewToDesktop(popupView, dimension, alwaysOnTop, false)
  }

  override def updateView(evt: SerChangeEvent) {
    var lastOccurredTime = masterSer.lastOccurredTime

    evt.lastObject match {
      case null =>
      case ticker: Ticker =>
        val percentValue = ticker.changeInPercent
        val strValue = ("%+3.2f%% " format percentValue) + ticker(Ticker.LAST_PRICE)
        val color = if (percentValue >= 0) LookFeel().getPositiveColor else LookFeel().getNegativeColor

        glassPane.updateInstantValue(strValue, color)
        prevClose = ticker(Ticker.PREV_CLOSE)

        val time = ticker.time
        if (time >= lastOccurredTime) {
          lastOccurredTime = time
        }
    }

    if (lastOccurredTime == 0) {
      cal.setTime(new Date)
      lastOccurredTime = cal.getTimeInMillis
    }

    adjustLeftSideRowToMarketOpenTime(lastOccurredTime)
  }

  private def adjustLeftSideRowToMarketOpenTime(time: Long) {
    val openTime  = market.openTime(time)
    val closeTime = market.closeTime(time)

    val begRow = masterSer.rowOfTime(openTime)
    val endRow = begRow + nBars - 1

    if (Null.is(prevClose)) {
      // @todo get precise prev *day* close
      val prevRow = masterSer.itemOfRow(begRow - 1).asInstanceOf[QuoteItem]
      if (prevRow != null) {
        prevClose = prevRow.close
        gridValues(0) = prevClose
      }
    }

    val lastOccurredRow = masterSer.lastOccurredRow
    controller.setCursorByRow(lastOccurredRow, endRow, true)
    //controller.updateViews();
  }
}


