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

import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.charting.view.scalar.LgScalar
import org.aiotrade.lib.charting.view.scalar.LinearScalar
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.securities.QuoteItem
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.Sec
import scala.collection.mutable.HashSet


/**
 *
 * @author Caoyuan Deng
 */
object AbstractQuoteChartView {
  def internal_switchAllQuoteChartType(originalType: QuoteChart.Type, targetType: QuoteChart.Type): QuoteChart.Type = {
    val newType =
      if (targetType != null) {
        targetType
      } else {
        originalType match {
          case QuoteChart.Type.Candle => QuoteChart.Type.Ohlc
          case QuoteChart.Type.Ohlc => QuoteChart.Type.Line
          case QuoteChart.Type.Line => QuoteChart.Type.Candle
          case _ => null
        }
      }

    newType
  }

}

abstract class AbstractQuoteChartView(controller: ChartingController, quoteSer: QuoteSer, empty: Boolean) extends ChartView with WithQuoteChart {
  import AbstractQuoteChartView._

  private var quoteChart:  QuoteChart = _
  protected var maxVolume, minVolume: Float = _
  protected var sec: Sec = _

  if (!empty) {
    init(controller, quoteSer)
  }
  
  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)

  override def init(controller: ChartingController, mainSer: TSer) {
    super.init(controller, mainSer)
    sec = controller.getContents.serProvider.asInstanceOf[Sec]
    if (axisXPane != null) {
      axisXPane.setTimeZone(sec.market.timeZone)
    }
  }

  def getQuoteSer: QuoteSer = {
    mainSer.asInstanceOf[QuoteSer]
  }

  protected def putChartsOfMainSer {
    quoteChart = new QuoteChart

    val vars = new HashSet[TVar[_]]
    mainSerChartMapVars.put(quoteChart, vars)
    for (v <- mainSer.vars) {
      if (v.plot == Plot.Quote) {
        vars.add(v)
      }
    }

    quoteChart.model.set(
      getQuoteSer.open,
      getQuoteSer.high,
      getQuoteSer.low,
      getQuoteSer.close)

    quoteChart.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT)
    mainChartPane.putChart(quoteChart)
  }

  override def computeMaxMin {
    var minValue1 = Math.MAX_FLOAT
    var maxValue1 = Math.MIN_FLOAT

    /** minimum volume should be 0 */
    minVolume = 0
    maxVolume = Math.MIN_FLOAT

    var i = 1
    while (i <= getNBars) {
      val time = tb(i)
      val item = mainSer.itemOf(time).asInstanceOf[QuoteItem]
      if (item != null && item.close > 0) {
        maxValue1 = Math.max(maxValue1, item.high)
        minValue1 = Math.min(minValue1, item.low)
        maxVolume = Math.max(maxVolume, item.volume)
      }

      i += 1
    }

    if (maxVolume == 0) {
      maxVolume = 1
    }

    if (maxVolume == minVolume) {
      maxVolume += 1
    }

    if (maxValue1 == minValue1) {
      maxValue1 *= 1.05f
      minValue1 *= 0.95f
    }

    setMaxMinValue(maxValue1, minValue1)
  }

  def getMaxVolume: Float = {
    maxVolume;
  }

  def getMinVolume: Float = {
    minVolume;
  }

  def getQuoteChart: QuoteChart = {
    quoteChart
  }

  def swithScalarType {
    getMainChartPane.getValueScalar.getType match {
      case Scalar.Type.Linear => setValueScalar(new LgScalar)
      case _ => setValueScalar(new LinearScalar)
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
}


