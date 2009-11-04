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

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.GeneralPath
import java.util.Calendar
import org.aiotrade.lib.charting.widget.HeavyPathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.math.StatisticFunction
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class ProfileChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _
        
    def set(v: TVar[_]) {
      this.v = v
    }
  }

  type M = Model

  private val heavyPathWidget = new HeavyPathWidget
  private val cal = Calendar.getInstance
  private var time: Long = _
    
  protected def createModel = new Model
    
  protected def plotChart {
  }
    
  override def render(g: Graphics) {
    val m = model
        
    val w = datumPlane.getWidth
    val h = datumPlane.getHeight
        
    val color = LookFeel.getCurrent.getGradientColor(getDepth, -10)
    setForeground(color)
        
    val width = (w * 2.386).intValue
        
    //color = Color.YELLOW;//new Color(1.0f, 1.0f, 1.0f, 0.618f)
        
    val controller = datumPlane.getView.getController
        
    time = controller.getReferCursorTime
        
    val xorigin = xb(bt(time))
        
    val path = heavyPathWidget.getPath(color)
        
    val item = ser.getItem(time)
        
    if (item != null) {
      item.get(m.v) match {
        case mass: Array[Array[Float]] =>
          plotProfileChart(mass, xorigin, width, path)
          g.setColor(color)
          g.asInstanceOf[Graphics2D].fill(path)
          //g.draw(path)
        case _ => null
      }
    }
        
    path.reset
  }
    
    
  private def plotProfileChart(profile: Array[Array[Float]], xorigin: Float, width: Float, path: GeneralPath) {
    val nIntervals = profile(StatisticFunction.VALUE).length - 1
        
    val halfInterval = if (nIntervals < 1) 0f else
      0.5f * (profile(StatisticFunction.VALUE)(1) - profile(StatisticFunction.VALUE)(0))
        
    var firstValueGot = false
        
    var y = Null.Float
    var i = 0
    while (i <= nIntervals) {
            
      val mass = profile(StatisticFunction.MASS)(i)
      if (Null.not(mass)) {
        val x = xorigin + mass * width
        y = yv(profile(StatisticFunction.VALUE)(i) + halfInterval)
        if (!firstValueGot) {
          path.moveTo(xorigin, y)
          firstValueGot = true
        } else {
          path.lineTo(x, y)
        }
                
      }

      i += 1
    }
        
    if (firstValueGot) {
      path.lineTo(xorigin, y)
      path.closePath
    }
  }
    
}



