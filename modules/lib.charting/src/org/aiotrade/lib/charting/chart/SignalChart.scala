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
import org.aiotrade.lib.charting.widget.Arrow
import org.aiotrade.lib.charting.widget.HeavyPathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.util.Sign
import org.aiotrade.lib.math.util.Signal

/**
 *
 * @author Caoyuan Deng
 */
class SignalChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[Signal] = _
        
    def set(v: TVar[Signal]) {
      this.v = v
    }
  }

  type M = Model
    
  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val positiveColor = LookFeel.getCurrent.getPositiveColor
    val negativeColor = LookFeel.getCurrent.getNegativeColor
        
    val color = Color.YELLOW
    setForeground(color)
        
    val heavyPathWidget = addChild(new HeavyPathWidget)
    val template = new Arrow
    var bar = 1
    while (bar <= nBars) {
            
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
                
        val signal = m.v.getByTime(time)
        if (signal != null) {
          val value = signal.value
          if (Null.not(value)) {
            val x = xb(bar)
            val y = yv(value)
                        
            signal.sign match {
              case Sign.EnterLong =>
                template.setForeground(color)
                template.model.set(x, y + 3, true, false)
              case Sign.ExitLong =>
                template.setForeground(color)
                template.model.set(x, y - 3, false, false)
              case Sign.EnterShort =>
                template.setForeground(color)
                template.model.set(x, y + 3, false, false)
              case Sign.ExitShort =>
                template.setForeground(color)
                template.model.set(x, y - 3, true, false)
              case _ =>
            }
            template.plot
            heavyPathWidget.appendFrom(template)
          }
        }

        i += 1
      }

      bar += nBarsCompressed
    }
        
  }
    
}



