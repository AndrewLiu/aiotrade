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
package org.aiotrade.lib.charting.chart

import java.awt.Color
import org.aiotrade.lib.charting.widget.CandleBar
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.charting.widget.HeavyPathWidget
import org.aiotrade.lib.charting.widget.OhlcBar
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar


/**
 *
 * @author Caoyuan Deng
 */
object QuoteChart {
  /**
   * Type will be got from the static property quoteChartType of view and we
   * should consider the case of view's repaint() being called, so we do not
   * include it in Model.
   */
  abstract class Type
  object Type {
    case object Candle extends Type
    case object Ohlc   extends Type
    case object Line   extends Type

    def values = Array(Candle, Ohlc, Line)
  }
}

class QuoteChart extends AbstractChart {
  import QuoteChart._

  class Model extends WidgetModel {
    var openVar:  TVar[Float] = _
    var highVar:  TVar[Float] = _
    var lowVar:   TVar[Float] = _
    var closeVar: TVar[Float] = _
        
    def set(openVar: TVar[Float], highVar: TVar[Float], lowVar: TVar[Float], closeVar: TVar[Float]) {
      this.openVar  = openVar
      this.highVar  = highVar
      this.lowVar   = lowVar
      this.closeVar = closeVar
    }
  }

  type M = Model

  private var positiveColor: Color = _
  private var negativeColor: Color = _
    

  protected def createModel: Model = new Model
    
  protected def plotChart {

    if (depth == Pane.DEPTH_DEFAULT) {
      positiveColor = LookFeel().getPositiveColor
      negativeColor = LookFeel().getNegativeColor
    } else {
      /** for comparing quotes charts */
      positiveColor = LookFeel().getChartColor(depth)
      negativeColor = positiveColor
    }
        
    val color = positiveColor
    setForeground(color)
        
    val tpe = datumPlane.view.asInstanceOf[WithQuoteChart].quoteChartType
    tpe match {
      case Type.Candle | Type.Ohlc =>
        plotCandleOrOhlcChart(tpe)
      case Type.Line =>
        plotLineChart

      case _ =>
    }
        
  }
    
  private def plotCandleOrOhlcChart(tpe: Type) {
    val m = model
        
    /**
     * @NOTICE
     * re-create and re-add children each time, so the children will release
     * its resource when reset();
     */
    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = if (tpe == Type.Candle) new CandleBar else new OhlcBar
    var bar = 1
    while (bar <= nBars) {
            
      /**
       * @TIPS:
       * use Null.Float to test if value has been set at least one time
       */
      var open  = Null.Float
      var close = Null.Float
      var high  = -Float.MaxValue
      var low   = +Float.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time) && m.closeVar(time) != 0) {
          if (Null.is(open)) {
            /** only get the first open as compressing period's open */
            open = m.openVar(time)
          }
          high  = Math.max(high, m.highVar(time))
          low   = Math.min(low,  m.lowVar (time))
          close = m.closeVar(time)
        }

        i += 1
      }
            
      if (Null.not(close) && close != 0) {
        val color = if (close >= open) positiveColor else negativeColor
                
        val yOpen  = yv(open)
        val yHigh  = yv(high)
        val yLow   = yv(low)
        val yClose = yv(close)
                
        tpe match {
          case Type.Candle =>
            val fillBar = LookFeel().isFillBar
            template.asInstanceOf[CandleBar].model.set(xb(bar), yOpen, yHigh, yLow, yClose, wBar, fillBar || close < open)
          case Type.Ohlc =>
            template.asInstanceOf[OhlcBar].model.set(xb(bar), yOpen, yHigh, yLow, yClose, wBar)
          case _ =>
        }
        template.setForeground(color)
        template.plot
        heavyPathWidget.appendFrom(template)
      }

      bar += nBarsCompressed
    }
        
  }
    
  private def plotLineChart {
    val m = model
        
    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = new LineSegment
    var y1 = Null.Float   // for prev
    var y2 = Null.Float   // for curr
    var bar = 1
    while (bar <= nBars) {
            
      /**
       * @TIPS:
       * use Null.Float to test if value has been set at least one time
       */
      var open  = Null.Float
      var close = Null.Float
      var max   = -Float.MaxValue
      var min   = +Float.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        val item = ser(time)
        if (item != null && item.getFloat(m.closeVar) != 0) {
          if (Null.is(open)) {
            /** only get the first open as compressing period's open */
            open = item.getFloat(m.openVar)
          }
          close = item.getFloat(m.closeVar)
          max = Math.max(max, close)
          min = Math.min(min, close)
        }

        i += 1
      }
            
      if (Null.not(close) && close != 0) {
        val color = if (close >= open) positiveColor else negativeColor
                
        y2 = yv(close)
        if (nBarsCompressed > 1) {
          /** draw a vertical line to cover the min to max */
          val x = xb(bar)
          template.model.set(x, yv(min), x, yv(max))
        } else {
          if (Null.not(y1)) {
            /**
             * x1 shoud be decided here, it may not equal prev x2:
             * think about the case of on calendar day mode
             */
            val x1 = xb(bar - nBarsCompressed)
            val x2 = xb(bar)
            template.model.set(x1, y1, x2, y2)
          }
        }
        y1 = y2
                
        template.setForeground(color)
        template.plot
        heavyPathWidget.appendFrom(template)
      }

      bar += 1
    }
        
  }
    
}
