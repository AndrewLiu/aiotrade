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
import org.aiotrade.lib.charting.widget.PathsWidget
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.WidgetModel
import java.awt.Font
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.signal.Direction
import org.aiotrade.lib.math.signal.Position
import org.aiotrade.lib.math.signal.Sign
import org.aiotrade.lib.math.signal.Signal

/**
 *
 * @author Caoyuan Deng
 */
class SignalChart extends AbstractChart {
  final class Model extends WidgetModel {
    var signalVar: TVar[List[Signal]] = _
    var highVar:   TVar[Double] = _
    var lowVar:    TVar[Double] = _

    def set(signalVar: TVar[List[Signal]], highVar: TVar[Double], lowVar: TVar[Double]) {
      this.signalVar = signalVar
      this.highVar   = highVar
      this.lowVar    = lowVar
    }
  }

  type M = Model
    
  protected def createModel = new Model

  protected def plotChart {
    val m = model
        
    val posColor = LookFeel().getPositiveColor
    val negColor = LookFeel().getNegativeColor
        
    val color = Color.YELLOW
    val antiColor = LookFeel().backgroundColor
    setForeground(color)

    val font = new Font(Font.DIALOG, Font.PLAIN, 10)
    val antiFont = new Font(Font.DIALOG, Font.BOLD, 8)

    val pathsWidget = addChild(new PathsWidget)
    val arrowTp = new Arrow
    var bar = 1
    while (bar <= nBars) {
            
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          var signals = m.signalVar(time)
          var j = 0
          var dyUp = 3
          var dyDn = 3
          while ((signals ne null) && (signals != Nil)) {
            signals = signals.reverse
            val signal = signals.head
            if (signal ne null) {
              val color = signal.color match {
                case null => Color.YELLOW
                case x => x
              }
              
              // appoint a reference value for this sign as the drawing position
              val refValue = if (m.lowVar != null && m.highVar != null) {
                signal.kind match {
                  case Direction.EnterLong | Direction.ExitShort  | Position.Lower => m.lowVar(time)
                  case Direction.ExitLong  | Direction.EnterShort | Position.Upper => m.highVar(time)
                  case _ => Null.Double
                }
              } else 0.0
                
              if (Null.not(refValue)) {
                val x = xb(bar)
                val y = yv(refValue)
                val text = signal.text

                signal.kind match {
                  case Direction.EnterLong | Direction.ExitShort | Position.Lower =>
                    var height = 12
                    var filled = false
                    if (signal.isInstanceOf[Sign]) {
                      arrowTp.setForeground(color)
                      arrowTp.model.set(x, y + dyUp, true, true)
                      height = math.max(height, 12)
                      filled = true
                    }

                    if (signal.hasText) {
                      val labelTp = addChild(new Label)
                      labelTp.setFont(if (filled) antiFont else font)
                      labelTp.setForeground(if (filled) antiColor else color)
                      labelTp.model.setText(text)
                      val bounds = labelTp.textBounds
                      labelTp.model.set(x - math.floor(bounds.width / 2.0).toInt, y + dyUp + bounds.height)
                      height = bounds.height
                    }

                    dyUp += (1 + height)
                  case Direction.ExitLong | Direction.EnterShort | Position.Upper =>
                    var height = 12
                    var filled = false
                    if (signal.isInstanceOf[Sign]) {
                      arrowTp.setForeground(color)
                      arrowTp.model.set(x, y - dyDn, false, true)
                      height = math.max(height, 12)
                      filled = true
                    }

                    if (signal.hasText) {
                      val labelTp = addChild(new Label)
                      labelTp.setFont(if (filled) antiFont else font)
                      labelTp.setForeground(if (filled) antiColor else color)
                      labelTp.model.setText(text)
                      val bounds = labelTp.textBounds
                      labelTp.model.set(x - math.floor(bounds.width / 2.0).toInt, y - dyDn - 3)
                      height = bounds.height
                    }
                    
                    dyDn += (1 + height)
                  case _ =>
                }
                
                if (signal.isInstanceOf[Sign]) {
                  arrowTp.plot
                  pathsWidget.appendFrom(arrowTp)
                }
              }
            }
            
            signals = signals.tail
            j += 1
          }
        }

        i += 1
      }

      bar += nBarsCompressed
    }
        
  }
    
}



