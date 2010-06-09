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

import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.util.Signal


/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 1, 2006, 3:04 AM
 * @since   1.0.4
 */
object ChartFactory {
    
  def createVarChart(v: TVar[_]): Chart = {
    var chart: Chart = null
    v.plot match  {
      case Plot.Volume =>
        chart = new VolumeChart
        chart.asInstanceOf[VolumeChart].model.set(false)
      case Plot.Line =>
        chart = new PolyLineChart
        chart.asInstanceOf[PolyLineChart].model.set(v.asInstanceOf[TVar[Float]])
      case Plot.Stick =>
        chart = new StickChart
        chart.asInstanceOf[StickChart].model.set(v.asInstanceOf[TVar[Float]])
      case Plot.Dot =>
        chart = new DotChart
        chart.asInstanceOf[DotChart].model.set(v)
      case Plot.Shade =>
        chart = new GradientChart
        chart.asInstanceOf[GradientChart].model.set(v, null)
      case Plot.Profile =>
        chart = new ProfileChart
        chart.asInstanceOf[ProfileChart].model.set(v)
      case Plot.Zigzag =>
        chart = new ZigzagChart
        chart.asInstanceOf[ZigzagChart].model.set(v)
      case Plot.Signal =>
        chart = new SignalChart
        chart.asInstanceOf[SignalChart].model.set((v.asInstanceOf[TVar[Signal]]))
      case _ =>
    }
        
    chart
  }
    
    
}
