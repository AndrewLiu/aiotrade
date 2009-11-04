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
package org.aiotrade.lib.charting.view

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.charting.chart.ChartFactory
import org.aiotrade.lib.charting.chart.GridChart
import org.aiotrade.lib.charting.chart.ProfileChart
import org.aiotrade.lib.charting.chart.GradientChart
import org.aiotrade.lib.charting.chart.StickChart
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.charting.view.pane.Pane
import scala.collection.mutable.HashSet


/**
 *
 * @author Caoyuan Deng
 */
class IndicatorChartView(controller: ChartingController, mainSer: TSer) extends ChartView {
    
  def this() = this(null, null)

  if (controller != null && mainSer != null) {
    init(controller, mainSer)
  }
    
  override def init(controller: ChartingController, mainSer: TSer) {
    super.init(controller, mainSer)
  }
    
  /**
   * Layout of IndicatorView
   *
   * title pane is intersect on chart pane's north
   * +----------------------------------------------------+-----+
   * |    title (0,0)                                     |     |
   * +----------------------------------------------------+     |
   * |    mainLayeredPane (0, 0)                          |     |
   * |    chartPane                                       |axisy|
   * |    drawingPane                                     |(1,0)|
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * +----------------------------------------------------+-----+
   * |    axisx                                                 |
   * +----------------------------------------------------------+
   */
  protected def initComponents {
    setLayout(new GridBagLayout)
    val gbc = new GridBagConstraints
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 100 - 100 / 6.18
    add(glassPane, gbc)
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 100 - 100 / 6.18
    add(mainLayeredPane, gbc)
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 0
    gbc.weighty = 100
    add(axisYPane, gbc)
        
    gbc.anchor = GridBagConstraints.SOUTH
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 2
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(divisionPane, gbc)
  }
    
    
  protected def putChartsOfMainSer {
    var depth = Pane.DEPTH_CHART_BEGIN;
    var depthGradient = Pane.DEPTH_GRADIENT_BEGIN
        
    for (ser <- getAllSers) {
      /** add charts */
      for (v <- ser.vars) {
        val chartVars = new HashSet[TVar[_]]
        val chart = ChartFactory.createVarChart(chartVars, v)
        if (chart != null) {
          mainSerChartMapVars.put(chart, chartVars)
                    
          chart.set(mainChartPane, ser)

          chart match {
            case _: GradientChart => chart.setDepth(depthGradient); depthGradient -= 1
            case _: ProfileChart => chart.setDepth(depthGradient); depthGradient -= 1
            case _: StickChart => chart.setDepth(-8)
            case _ => chart.setDepth(depth); depth += 1
          }
                    
          mainChartPane.putChart(chart)
        }
      }
            
      /** plot grid */
      val grids = mainSer.asInstanceOf[Indicator].grids
      if (grids != null && grids.length > 0) {
        val gridChart = new GridChart
                
        gridChart.model.set(grids, GridChart.Direction.Horizontal)
        gridChart.set(mainChartPane, null, Pane.DEPTH_DRAWING)
                
        mainChartPane.putChart(gridChart)
      }
    }
  }
    
  override def computeMaxMin {
    var minValue1 = +Math.MAX_FLOAT
    var maxValue1 = -Math.MAX_FLOAT
        
    var i = 1
    while (i <= getNBars) {
      val time = tb(i)
      val item = mainSer.getItem(time)
      if (item != null) {
        for (v <- mainSer.vars) {
          if (v.plot != Plot.None) {
            val value = item.getFloat(v)
            if (value != Null.Float) {
              maxValue1 = Math.max(maxValue1, value)
              minValue1 = Math.min(minValue1, value)
            }
          }
        }
      }

      i += 1
    }
        
    if (maxValue1 == minValue1) {
      maxValue1 += 1
    }
        
    setMaxMinValue(maxValue1, minValue1)
  }
    
  override def popupToDesktop {
    val popupView = new PopupIndicatorChartView(getController, getMainSer)
    val alwaysOnTop = true;
    val dimension = new Dimension(getWidth, 200)
        
    getController.popupViewToDesktop(popupView, dimension, alwaysOnTop, false)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
}


