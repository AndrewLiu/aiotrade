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
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.securities.QuoteItem
import org.aiotrade.lib.securities.QuoteSer


/**
 *
 * @author Caoyuan Deng
 */
class VolumeChart extends AbstractChart {

  final class Model extends WidgetModel {

    var thin: Boolean = _

    def set(thin: Boolean) {
      this.thin = thin
    }
  }

  type M = Model

  protected def createModel = new Model

  protected def plotChart {
    assert(masterSer.isInstanceOf[QuoteSer], "VolumeChart's masterSer should be QuoteSer!")

    val m = model

    val thin = LookFeel().isThinVolumeBar || m.thin

    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = new StickBar
    var y1 = yv(0)
    var bar = 1
    while (bar <= nBars) {

      var open   = Null.Float
      var close  = Null.Float
      var high   = -Math.MAX_FLOAT
      var low    = +Math.MAX_FLOAT
      var volume = -Math.MAX_FLOAT // we are going to get max of volume during nBarsCompressed
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        val item = masterSer(time).asInstanceOf[QuoteItem]
        if (item != null && item.close != 0) {
          if (Null.is(open)) {
            /** only get the first open as compressing period's open */
            open = item.open
          }
          high   = Math.max(high, item.high)
          low    = Math.min(low,  item.low)
          close  = item.close
          volume = Math.max(volume, item.volume)
        }

        i += 1
      }

      if (volume >= 0 /* means we've got volume value */) {
        val color = if (close >= open) LookFeel().getPositiveColor else LookFeel().getNegativeColor
        setForeground(color)
                
        val xCenter = xb(bar)
        val y2 = yv(volume)

        template.setForeground(color)
        val fillBar = LookFeel().isFillBar
        template.model.set(xCenter, y1, y2, wBar, thin, fillBar || close < open)
        template.plot
        heavyPathWidget.appendFrom(template)
      }

      bar += nBarsCompressed
    }
  }
}
