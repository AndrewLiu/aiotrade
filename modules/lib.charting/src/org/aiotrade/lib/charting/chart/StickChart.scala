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

import org.aiotrade.lib.charting.widget.HeavyPathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.widget.StickBar
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class StickChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[Float] = _
        
    def set(v: TVar[Float]) {
      this.v = v
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val positiveColor = LookFeel().getPositiveColor
    val negativeColor = LookFeel().getNegativeColor
        
    var color = positiveColor
    setForeground(color)
        
    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = new StickBar
    var bar = 1
    while (bar <= nBars) {
      var max = -Float.MaxValue
      var min = +Float.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          val value = m.v(time)
          max = math.max(max, value)
          min = math.min(min, value)
        }

        i += 1
      }
            
            
      max = math.max(max, 0) // max not less than 0
      min = math.min(min, 0) // min not more than 0

      if (! (max == 0 && min == 0)) {
        var yValue = 0f
        var yDatum = 0f
        if (math.abs(max) > math.abs(min)) {
          color = positiveColor
          yValue = yv(max)
          yDatum = yv(min)
        } else {
          color = negativeColor
          yValue = yv(min)
          yDatum = yv(max)
        }
                
        val x = xb(bar)
        template.setForeground(color)
        template.model.set(x, yDatum, yValue, wBar, true, false)
        template.plot
        heavyPathWidget.appendFrom(template)

        if (x % AbstractChart.MARK_INTERVAL == 0) {
          addMarkPoint(x.toInt, yValue.toInt)
        }
      }

      bar += nBarsCompressed
    }
        
  }
    
}

