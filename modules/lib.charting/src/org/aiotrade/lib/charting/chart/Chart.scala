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

import org.aiotrade.lib.charting.view.pane.DatumPlane
import org.aiotrade.lib.charting.widget.Widget
import org.aiotrade.lib.math.timeseries.TSer


/**
 *
 * @author Caoyuan Deng
 */
object Chart {
  abstract class StrockType
  object StrockType {
    case object Base extends StrockType
    case object Dash extends StrockType
  }
}

trait Chart extends Widget with Ordered[Chart] {
  import Chart._

  /**
   * @NOTICE:
   * It's always better to set datumPlane here. After call following set(,,,)
   * methods, the chart can be properly put in any datumPlane with the same datum,
   * by calling DatumPlane.putChart() for automatically rendering, or, can be
   * drawn on pane by calling render() initiatively (such as mouse cursor chart).
   * So, do not try to separate a setDatumPane(AbstractDatumPlane) method.
   */
  def set(datumPlane: DatumPlane, ser: TSer, depth: Int): Unit
  def set(datumPlane: DatumPlane, ser: TSer): Unit
    
  def setFirstPlotting(b: Boolean): Unit
  def isFirstPlotting: Boolean
    
  def setDepth(depth: Int): Unit
  def getDepth: Int
    
  def setSer(ser: TSer): Unit
  def getSer: TSer
    
    
  def setStrock(strockWidth: Int, strockType: StrockType): Unit
  def getStrockWidth: Float
  def getStrockType: StrockType
    
  def isSelected: Boolean
  def setSelected(b: Boolean): Unit
    
  def reset: Unit
    
}
