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
import org.aiotrade.lib.charting.widget.PathWidget

/**
 *
 * @author Caoyuan Deng
 */
class ParallelLinesChart extends AbstractChart {
  final class Model extends WidgetModel {
    var t1: Long = _
    var v1: Float = _
    var t2: Long = _
    var v2: Float = _
    var t3: Long = _
    var v3: Float = _
        
    def set(t1: Long, v1: Float, t2: Long, v2: Float, t3: Long, v3: Float) {
      this.t1 = t1
      this.v1 = v1
      this.t2 = t2
      this.v2 = v2
      this.t3 = t3
      this.v3 = v3
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel.getCurrent.drawingColor
    setForeground(color)
        
    val xs = new Array[Float](3)
    val ys = new Array[Float](3)
    xs(0) = xb(bt(m.t1))
    xs(1) = xb(bt(m.t2))
    xs(2) = xb(bt(m.t3))
    ys(0) = yv(m.v1)
    ys(1) = yv(m.v2)
    ys(2) = yv(m.v3)
        
    val dx = xs(1) - xs(0)
    val dy = ys(1) - ys(0)
        
    val k = if (dx == 0) 1F else dy / dx
        
    val distance = Math.abs(k * xs(2) - ys(2) + ys(0) - k * xs(0)) / Math.sqrt(k * k + 1)
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
        
    plotLine(xs(0), ys(0), k, path)
        
    if (distance >= 1) {
      plotLine(xs(2), ys(2), k, path)
    }
        
  }
    
}


