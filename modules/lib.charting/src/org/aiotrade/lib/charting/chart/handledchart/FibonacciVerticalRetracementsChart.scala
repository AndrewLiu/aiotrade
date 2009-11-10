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
package org.aiotrade.lib.charting.chart.handledchart

import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.chart.AbstractChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.PathWidget

/**
 *
 * @author Caoyuan Deng
 */
class FibonacciVerticalRetracementsChart extends AbstractChart {
  final class Model extends WidgetModel {
    var t1: Long = _
    var t2: Long = _
        
    def set(t1: Long, t2: Long) {
      this.t1 = t1
      this.t2 = t2
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel().drawingColor
    setForeground(color)
        
    val bs = new Array[Float](2)
    bs(0) = bt(m.t1)
    bs(1) = bt(m.t2)
    val interval = bs(1) - bs(0)
        
    /** calculate Fibonacci serials */
    val Fn = Array(
      0.000f,
      0.382f,
      0.500f,
      0.618f,
      1.000f,
      1.236f,
      1.618f,
      2.000f,
      2.618f,
      3.000f
    )
        
        
    val ymin = yv(datumPlane.minValue)
    var n = 0
    while (n < Fn.length) {
      val b = Math.round(bs(0) + interval * Fn(n))
      if (b >= 1 && b <= nBars) {
        val pathWidget = addChild(new PathWidget)
        pathWidget.setForeground(color)
        val path = pathWidget.getPath
        plotVerticalLine(b, path)
                
        val x = xb(b)
        val label1 = addChild(new Label)
        label1.setFont(LookFeel().axisFont)
        label1.setForeground(color)
        label1.model.set(x + 1, ymin, Fn(n).toString)
        label1.plot
      }

      n += 1
    }
  }
    
}


