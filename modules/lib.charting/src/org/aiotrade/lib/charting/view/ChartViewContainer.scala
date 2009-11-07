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

import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import scala.collection.mutable.HashMap


/**
 *
 * @author Caoyuan Deng
 */
abstract class ChartViewContainer extends JPanel {

  private val descriptorsToSlaveView = new HashMap[IndicatorDescriptor, ChartView]
  private var controller: ChartingController = _
  private var masterView: ChartView = _
  /**
   * each viewContainer can only contains one selectedChart, so we define it here instead of
   * on ChartView or ChartPane;
   */
  private var selectedChart: Chart = _
  private var selectedView: ChartView = _
  private var interactive = true
  private var pinned = false
  private var _parent: Component = _

  /**
   * init this viewContainer instance. binding with controller (so, MaserSer and Descriptor) here
   */
  def init(focusableParent: Component, controller: ChartingController) {
    this._parent = focusableParent
    this.controller = controller

    initComponents
  }

  override def paint(g: Graphics) {
    super.paint(g)
  }

  protected def initComponents: Unit

  def getController: ChartingController = {
    controller
  }

  def setInteractive(b: Boolean) {
    getMasterView.setInteractive(b)

    for (view <- descriptorsToSlaveView.valuesIterator) {
      view.setInteractive(b)
    }

    this.interactive = b
  }

  /**
   * It's just an interactive hint, the behave for interactive will be defined
   * by ChartView(s) and its Pane(s).
   *
   * @return true if the mouse will work interacticely, false else.
   */
  def isInteractive: Boolean = {
    interactive
  }

  def pin {
    getMasterView.pin

    this.pinned = true
  }

  def unPin {
    getMasterView.unPin

    this.pinned = false
  }

  def isPinned: Boolean = {
    pinned
  }

  def adjustViewsHeight(increment: Int) {
    /**
     * @TODO
     * Need implement adjusting each views' height ?
     */
    val gbl = getLayout.asInstanceOf[GridBagLayout]
    val gbc = new GridBagConstraints
    gbc.fill = GridBagConstraints.BOTH
    gbc.gridx = 0
    gbc.weightx = 100

    var numSlaveViews = 0
    var sumSlaveViewsHeight = 0f
    for (view <- descriptorsToSlaveView.valuesIterator if view != getMasterView) {
      /** overlapping view is also in masterView, should ignor it */
      sumSlaveViewsHeight += view.getHeight
      numSlaveViews += 1
    }

    if (numSlaveViews == 1 && sumSlaveViewsHeight == 0) {
      /** first slaveView added */
      sumSlaveViewsHeight = 0.382f * masterView.getHeight
    }

    setVisible(false)

    val adjustHeight = increment
    gbc.weighty = masterView.getHeight + adjustHeight

    /**
     * We need setConstraints and setSize together to take the effect
     * according to GridBagLayout's behave.
     * We can setSize(new Dimension(0, 0)) and let GridBagLayout arrange
     * the size according to weightx and weighty, but for performence issue,
     * we'd better setSize() to the actual size that we want.
     */
    gbl.setConstraints(masterView, gbc)
    masterView.setSize(new Dimension(masterView.getWidth, gbc.weighty.toInt))
    for (view <- descriptorsToSlaveView.valuesIterator if view != getMasterView) {
      /** average assigning */
      gbc.weighty = (sumSlaveViewsHeight - adjustHeight) / numSlaveViews
      /*-
       * proportional assigning
       * gbc.weighty = v.getHeight() - adjustHeight * v.getHeight() / iHeight;
       */
      gbl.setConstraints(view, gbc)
      view.setSize(new Dimension(view.getWidth, gbc.weighty.toInt))
    }

    setVisible(true)
  }

  def getMasterView: ChartView = {
    masterView
  }

  protected def setMasterView(masterView: ChartView, gbc: GridBagConstraints) {
    this.masterView = masterView
    add(masterView, gbc)
  }

  def addSlaveView(descriptor: IndicatorDescriptor, indicator: Indicator, agbc: GridBagConstraints) {
    var gbc = agbc
    if (!descriptorsToSlaveView.contains(descriptor)) {
      var view: ChartView = null
      if (indicator.isOverlapping) {
        view = getMasterView
        view.addOverlappingCharts(indicator)
      } else {
        view = new IndicatorChartView(getController, indicator)
        if (gbc == null) {
          gbc = new GridBagConstraints
          gbc.fill = GridBagConstraints.BOTH
          gbc.gridx = 0
        }
        add(view, gbc)
      }
      descriptorsToSlaveView.put(descriptor, view)
      setSelectedView(view)
    }
  }

  def removeSlaveView(descriptor: IndicatorDescriptor) {
    val view = lookupChartView(descriptor) match {
      case Some(view) if view == getMasterView =>
        view.removeOverlappingCharts(descriptor.createdServerInstance().get)
      case Some(view) =>
        remove(view)
        adjustViewsHeight(0)
        view.getAllSers.clear
        repaint()
      case None =>
    }
    descriptorsToSlaveView.remove(descriptor)
  }

  def getSlaveViews = {
    descriptorsToSlaveView.valuesIterator
  }

  def setSelectedView(view: ChartView) {
    if (selectedView != null) {
      selectedView.setSelected(false)
    }

    if (view != null) {
      selectedView = view
      selectedView.setSelected(true)
    } else {
      selectedView = null
    }
  }

  def getSelectedView: ChartView = {
    selectedView
  }

  def getSelectedChart: Chart = {
    selectedChart
  }

  /**
   * @param chart the chart to be set as selected, could be <b>null</b>
   */
  def setSelectedChart(chart: Chart) {
    if (selectedChart != null) {
      selectedChart.setSelected(false)
    }

    if (chart != null) {
      selectedChart = chart
      selectedChart.setSelected(true)
    } else {
      selectedChart = null
    }

    repaint()
  }

  def lookupIndicatorDescriptor(view: ChartView): IndicatorDescriptor = {
    for (descriptor <- descriptorsToSlaveView.keysIterator) {
      val theView = descriptorsToSlaveView.get(descriptor)
      if (theView != null && theView == view) {
        return descriptor
      }
    }
    null
  }

  def lookupChartView(descriptor: IndicatorDescriptor): Option[ChartView] = {
    descriptorsToSlaveView.get(descriptor)
  }

  def getDescriptorsWithSlaveView: HashMap[IndicatorDescriptor, ChartView] = {
    descriptorsToSlaveView
  }

  def getFocusableParent: Component = {
    _parent
  }

  @throws(classOf[Exception])
  def saveToCustomSizeImage(file: File, fileFormat: String, width: Int, height: Int) {
    /** backup: */
    val backupRect = getBounds()

    setBounds(0, 0, width, height)
    validate

    saveToImage(file, fileFormat)

    /** restore: */
    setBounds(backupRect)
    validate
  }

  @throws(classOf[Exception])
  def saveToCustomSizeImage(file: File, fileFormat: String, begTime: Long, endTime: Long, height: Int) {
    val begPos = controller.getMasterSer.rowOfTime(begTime)
    val endPos = controller.getMasterSer.rowOfTime(endTime)
    val nBars = endPos - begPos
    val width = (nBars * controller.getWBar).toInt

    /** backup: */
    val backupRightCursorPos = controller.getRightSideRow
    val backupReferCursorPos = controller.getReferCursorRow

    controller.setCursorByRow(backupReferCursorPos, endPos, true)

    saveToCustomSizeImage(file, fileFormat, width, height)

    /** restore: */
    controller.setCursorByRow(backupReferCursorPos, backupRightCursorPos, true)
  }

  @throws(classOf[Exception])
  def saveToImage(file: File, fileFormat: String) {
    val fileName = (file.toString + ".png")

    if (masterView.getXControlPane != null) {
      masterView.getXControlPane.setVisible(false)
    }

    if (masterView.getYControlPane != null) {
      masterView.getYControlPane.setVisible(false)
    }

    val image = paintToImage

    ImageIO.write(image, fileFormat, file)

    if (masterView.getXControlPane != null) {
      masterView.getXControlPane.setVisible(true)
    }

    if (masterView.getYControlPane != null) {
      masterView.getYControlPane.setVisible(true)
    }
  }

  @throws(classOf[Exception])
  def paintToImage: RenderedImage = {
    val renderImage = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_RGB)

    val gImg = renderImage.createGraphics
    try {
      paint(gImg);
    } catch {case ex: Exception => throw ex
    } finally {gImg.dispose}

    renderImage
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    descriptorsToSlaveView.clear
    super.finalize
  }
}