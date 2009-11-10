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
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.charting.view.pane.XControlPane

/**
 *
 * @author Caoyuan Deng
 */
class PopupIndicatorChartView(acontroller: ChartingController,
                              amainSer: TSer,
                              empty: Boolean
) extends IndicatorChartView(acontroller, amainSer, empty) {

  def this(controller: ChartingController, mainSer: TSer) = this(controller, mainSer, false)
  def this() = this(null, null, true)
    
  override protected def initComponents {
    xControlPane = new XControlPane(this, mainChartPane)
    xControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    /** begin to set the layout: */
        
    setLayout(new GridBagLayout)
    val gbc = new GridBagConstraints
        
    /**
     * @NOTICE be ware of the components added order:
     * 1. add xControlPane, it will partly cover glassPane in SOUTH,
     * 2. add glassPane, it will exactly cover mainLayeredPane
     * 3. add mainLayeredPane.
     *
     * After that, xControlPane can accept its self mouse events, and so do
     * glassPane except the SOUTH part covered by xControlPane.
     *
     * And glassPane will forward mouse events to whom it covered.
     * @see GlassPane#processMouseEvent(MouseEvent) and
     *      GlassPane#processMouseMotionEvent(MouseEvent)
     */
        
    gbc.anchor = GridBagConstraints.SOUTH
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(xControlPane, gbc)
        
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
        
    /**
     * add the axisYPane in the same grid as yControlPane then, it will be
     * covered by yControlPane partly in SOUTH
     */
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.weightx = 0
    gbc.weighty = 100
    add(axisYPane, gbc)
        
    /** add axisXPane and dividentPane across 2 gridwidth horizontally, */
        
    gbc.anchor = GridBagConstraints.CENTER
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 2
    gbc.gridheight = 1
    gbc.weightx = 100
    gbc.weighty = 0
    add(axisXPane, gbc)
  }
    
  override def xControlPane: XControlPane = xControlPane
}



