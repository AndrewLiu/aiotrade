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
package org.aiotrade.lib.charting.chart.handledchart;

import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.charting.widget.Arc
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.chart.AbstractChart
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class FibonacciArcsChart extends AbstractChart {
  final class Model extends WidgetModel {
    var t1: Long = _
    var v1: Float = _
    var t2: Long = _
    var v2: Float = _
        
    def set(t1: Long, v1: Float, t2: Long, v2: Float) {
      this.t1 = t1
      this.v1 = v1
      this.t2 = t2
      this.v2 = v2
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel.getCurrent.drawingColor
    setForeground(color)
        
    val xs = new Array[float](2)
    val ys = new Array[float](2)
    xs(0) = xb(bt(model.t1))
    xs(1) = xb(bt(model.t2))
    ys(0) = yv(model.v1)
    ys(1) = yv(model.v2)
        
    val line = addChild(new LineSegment)
    line.setForeground(color)
    line.model.set(xs(0), ys(0), xs(1), ys(1))
    line.plot
        
    val dx = xs(1) - xs(0)
    val dy = ys(1) - ys(0)
    val radius = Math.sqrt(dx * dx + dy * dy).floatValue
        
    val radius1 = radius * 0.382f
    val radius2 = radius * 0.500f
    val radius3 = radius * 0.618f
    val radius4 = radius * 0.763f
        
    val arc1 = addChild(new Arc)
    arc1.setForeground(color);
    arc1.model.set(xs(1) - radius1, ys(1) - radius1, radius1 * 2f, radius1 * 2f, 0f, 360f, 0)
    arc1.plot
        
    val arc2 = addChild(new Arc)
    arc2.setForeground(color);
    arc2.model.set(xs(1) - radius2, ys(1) - radius2, radius2 * 2f, radius2 * 2f, 0f, 360f, 0)
    arc2.plot
        
    val arc3 = addChild(new Arc)
    arc3.setForeground(color);
    arc3.model.set(xs(1) - radius3, ys(1) - radius3, radius3 * 2f, radius3 * 2f, 0f, 360f, 0)
    arc3.plot
        
    val arc4 = addChild(new Arc)
    arc4.setForeground(color);
    arc4.model.set(xs(1) - radius4, ys(1) - radius4, radius4 * 2f, radius4 * 2f, 0f, 360f, 0)
    arc4.plot
        
    val k = if (dx == 0) 1f else dy / dx
        
    var xText = xs(1) - dx * 0.382f + 2
    var yText = GeomUtil.yOfLine(xText, xs(0), ys(0), k)
    val label1 = addChild(new Label)
    label1.setFont(LookFeel.getCurrent.axisFont)
    label1.setForeground(color)
    label1.model.set(2 * xs(1) - xText, yText, "0.382")
    label1.plot
        
    xText = xs(1) - dx * 0.500f + 2
    yText = GeomUtil.yOfLine(xText, xs(0), ys(0), k)
    val  label2 = addChild(new Label)
    label2.setFont(LookFeel.getCurrent.axisFont)
    label2.setForeground(color)
    label2.model.set(2 * xs(1) - xText, yText, "0.5")
    label2.plot
        
    xText = xs(1) - dx * 0.618f + 2
    yText = GeomUtil.yOfLine(xText, xs(0), ys(0), k)
    val label3 = addChild(new Label)
    label3.setFont(LookFeel.getCurrent.axisFont);
    label3.setForeground(color)
    label3.model.set(2 * xs(1) - xText, yText, "0.618")
    label3.plot
        
    xText = xs(1) - dx * 0.763f + 2
    yText = GeomUtil.yOfLine(xText, xs(0), ys(0), k)
    val label4 = addChild(new Label)
    label4.setFont(LookFeel.getCurrent.axisFont)
    label4.setForeground(color)
    label4.model.set(2 * xs(1) - xText, yText, "0.763")
    label4.plot
  }
    
    
}




