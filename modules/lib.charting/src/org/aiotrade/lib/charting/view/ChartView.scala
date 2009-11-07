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
import java.awt.event.ComponentAdapter
import javax.swing.JLayeredPane
import java.awt.Graphics
import javax.swing.JComponent
import org.aiotrade.lib.charting.view.pane.AxisXPane
import org.aiotrade.lib.charting.view.pane.AxisYPane
import org.aiotrade.lib.charting.view.pane.ChartPane
import org.aiotrade.lib.charting.view.pane.DivisionPane
import org.aiotrade.lib.charting.view.pane.GlassPane
import org.aiotrade.lib.math.timeseries.SerChangeEvent
import org.aiotrade.lib.math.timeseries.SerChangeListener
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.charting.chart.ChartFactory
import org.aiotrade.lib.charting.chart.GradientChart
import org.aiotrade.lib.charting.chart.ProfileChart
import org.aiotrade.lib.charting.chart.StickChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.view.pane.XControlPane
import org.aiotrade.lib.charting.view.pane.YControlPane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.util.ChangeObservable
import org.aiotrade.lib.util.ChangeObserver
import org.aiotrade.lib.util.ChangeObservableHelper
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedHashMap


/**
 * A ChartView's container can be any Component even without a ChartViewContainer,
 * but should reference back to a controller. All ChartViews shares the same
 * controller will have the same cursor behaves.
 *
 * Example: you can add a ChartView directly to a JFrame.
 *
 * masterSer: the ser instaceof MasterSer, with the calendar time feature,
 *            it's put in the masterView to control the cursor;
 * mainSer: vs overlappingSer, this view's main ser.
 *
 *       1..n           1..n
 * ser --------> chart ------> var
 *
 * @author Caoyuan Deng
 */
abstract class ChartView(protected var controller: ChartingController, protected var mainSer: TSer, empty: Boolean) extends {
  val AXISX_HEIGHT = 12
  val AXISY_WIDTH = 50
  val CONTROL_HEIGHT = 12
  val TITLE_HEIGHT_PER_LINE = 12
} with JComponent with ChangeObservable {

  protected var masterSer: MasterTSer = _
  protected val mainSerChartMapVars = new LinkedHashMap[Chart, HashSet[TVar[_]]]
  protected val overlappingSerChartMapVars = new LinkedHashMap[TSer, LinkedHashMap[Chart, HashSet[TVar[_]]]]
  protected var lastDepthOfOverlappingChart = Pane.DEPTH_CHART_BEGIN
  protected var mainChartPane: ChartPane = _
  protected var glassPane: GlassPane = _
  protected var axisXPane: AxisXPane = _
  protected var axisYPane: AxisYPane = _
  protected var divisionPane: DivisionPane = _
  protected var xControlPane: XControlPane = _
  protected var yControlPane: YControlPane = _
  protected var mainLayeredPane: JLayeredPane = _
  /** geometry */
  private var nBars: Int = _ // number of bars
  private var maxValue = 1f
  private var minValue = 0f
  private var oldMaxValue = maxValue
  private var oldMinValue = minValue

  private var componentAdapter: ComponentAdapter = _
  private var interactive = true
  private var pinned = false
  private val observableHelper = new ChangeObservableHelper
  protected val serChangeListener = new MySerChangeListener

  if (!empty) {
    init(controller, mainSer)
  }

  def this(controller: ChartingController, mainSer: TSer) = this(controller, mainSer, false)
  def this() = this(null, null, true)

  def init(controller: ChartingController, mainSer: TSer) {
    this.controller = controller
    this.masterSer = controller.getMasterSer
    this.mainSer = mainSer

    createBasisComponents

    initComponents

    putChartsOfMainSer

    this.mainSer.addSerChangeListener(serChangeListener)

    /** @TODO should consider: in case of overlapping indciators, how to avoid multiple repaint() */
  }

  def addObserver(owner: Object, observer: ChangeObserver[Any]) {
    observableHelper.addObserver(owner, observer)
  }

  def removeObserver(observer: ChangeObserver[Any]) {
    observableHelper.removeObserver(observer)
  }

  def removeObserversOf(owner: Object) {
    observableHelper.removeObserversOf(owner)
  }

  /**
   * Changed cases:
   *   rightSideRow
   *   referCursorRow
   *   wBar
   *   onCalendarMode
   */
  def notifyObserversChanged(oberverType: Class[_ <: ChangeObserver[Any]]) {
    observableHelper.notifyObserversChanged(this, oberverType)
  }

  protected def initComponents: Unit

  private def createBasisComponents {
    setDoubleBuffered(true)

    /**
     * !NOTICE
     * To make background works, should keep three conditions:
     * 1. It should be a JPanel instead of a JComponent(which may has no background);
     * 2. It should be opaque;
     * 3. If override paintComponent(g0), should call super.paintComponent(g0) ?
     */
    setOpaque(true)

    mainChartPane = new ChartPane(this)
    glassPane = new GlassPane(this, mainChartPane)
    axisXPane = new AxisXPane(this, mainChartPane)
    axisYPane = new AxisYPane(this, mainChartPane)
    divisionPane = new DivisionPane(this, mainChartPane)

    mainLayeredPane = new JLayeredPane {

      /** this will let the pane components getting the proper size when init */
      override protected def paintComponent(g: Graphics) {
        val width = getWidth
        val height = getHeight
        for (c <- getComponents if c.isInstanceOf[Pane]) {
          c.setBounds(0, 0, width, height)
        }
      }
    }
    mainLayeredPane.setPreferredSize(new Dimension(10, (10 - 10 / 6.18).toInt))
    mainLayeredPane.add(mainChartPane, JLayeredPane.DEFAULT_LAYER)

    glassPane.setPreferredSize(new Dimension(10, (10 - 10 / 6.18).toInt))

    axisXPane.setPreferredSize(new Dimension(10, AXISX_HEIGHT))
    axisYPane.setPreferredSize(new Dimension(AXISY_WIDTH, 10))
    divisionPane.setPreferredSize(new Dimension(10, 1))
  }

  /**
   * The paintComponent() method will always be called automatically whenever
   * the component need to be reconstructed as it is a JComponent.
   */
  override protected def paintComponent(g: Graphics) {
    prePaintComponent

    if (isOpaque) {
      /**
       * Process background by self,
       *
       * @NOTICE
       * don't forget to setBackgroud() to keep this component's properties consistent
       */
      setBackground(LookFeel().backgroundColor)
      g.setColor(getBackground)
      g.fillRect(0, 0, getWidth, getHeight)
    }

    /**
     * @NOTICE:
     * if we call:
     *   super.paintComponent(g);
     * here, this.paintComponent(g) will be called three times!!!, the reason
     * may be that isOpaque() == true
     */
    postPaintComponent
  }

  protected def prePaintComponent {
    computeGeometry

    /** @TODO, use notify ? */
    getMainChartPane.computeGeometry
  }

  /**
   * what may affect the geometry:
   * 1. the size of this component changed;
   * 2. the rightCursorRow changed;
   * 3. the ser's value changed or its items added, which need computeMaxMin();
   *
   * The controller only define wBar (the width of each bar), this component
   * will compute number of bars according to its size. So, if you want to more
   * bars displayed, such as an appointed newNBars, you should compute the size of
   * this's container, and call container.setBounds() to proper size, then, the
   * layout manager will layout the size of its ChartView instances automatically,
   * and if success, the newNBars computed here will equals the newNBars you want.
   */
  protected def computeGeometry {
    /**
     * @NOTICE
     * 1.Should get wBar firstly, then calculator nBars
     * 2.Get this view's width to compute nBars instead of mainChartPane's
     * width, because other panes may be repainted before mainChartPane is
     * properly layouted (the width of mainChartPane is still not good)
     */
    val newNBars = ((getWidth - AXISY_WIDTH) / controller.getWBar).intValue

    /** avoid nBars == 0 */
    setNBars(Math.max(newNBars, 1))

    /**
     * We only need computeMaxMin() once when a this should be repainted,
     * so do it here.
     */
    computeMaxMin
    if (maxValue != oldMaxValue || minValue != oldMinValue) {
      oldMaxValue = maxValue
      oldMinValue = minValue
      notifyObserversChanged(classOf[ChartValidityObserver[Any]])
    }
  }

  protected def postPaintComponent {
    /**
     * update controlPane's scrolling thumb position etc.
     *
     * @NOTICE
     * We choose here do update controlPane, because the paint() called in
     * Java Swing is async, we not sure when it will be really called from
     * outside, even in this's container, so here is relative safe place to
     * try, because here means the paint() is truely beging called by awt.
     */
    if (getAxisXPane != null) {
      getAxisXPane.syncWithView
    }

    if (getAxisYPane != null) {
      getAxisYPane.syncWithView
    }

    if (getXControlPane != null) {
      getXControlPane.syncWithView
    }

    if (getYControlPane != null) {
      getYControlPane.syncWithView
    }

  }

  private def setNBars(nBars: Int) {
    val oldValue = this.nBars
    this.nBars = nBars
    if (this.nBars != oldValue) {
      notifyObserversChanged(classOf[ChartValidityObserver[Any]])
    }
  }

  protected def setMaxMinValue(max: Float, min: Float) {
    maxValue = max
    minValue = min
  }

  def setSelected(b: Boolean) {
    getGlassPane.setSelected(b)
  }

  def setInteractive(b: Boolean) {
    getGlassPane.setInteractive(b)

    this.interactive = b
  }

  def isInteractive: Boolean = {
    return interactive
  }

  def pin {
    getGlassPane.setPinned(true);

    this.pinned = true
  }

  def unPin {
    getGlassPane.setPinned(false)

    this.pinned = false
  }

  def isPinned: Boolean = {
    pinned;
  }

  def setYChartScale(yChartScale: Float) {
    val datumPane = getMainChartPane
    if (datumPane != null) {
      datumPane.setYChartScale(yChartScale)
    }

    repaint()
  }

  def setValueScalar(valueScalar: Scalar) {
    val datumPane = getMainChartPane
    if (datumPane != null) {
      datumPane.setValueScalar(valueScalar)
    }

    repaint()
  }

  def adjustYChartScale(increment: Float) {
    val datumPane = getMainChartPane
    if (datumPane != null) {
      datumPane.growYChartScale(increment)
    }

    repaint();
  }

  def setYChartScaleByCanvasValueRange(canvasValueRange: Double) {
    val datumPane = getMainChartPane
    if (datumPane != null) {
      datumPane.setYChartScaleByCanvasValueRange(canvasValueRange)
    }

    repaint()
  }

  def scrollChartsVerticallyByPixel(increment: Int) {
    val datumPane = getMainChartPane
    if (datumPane != null) {
      datumPane.scrollChartsVerticallyByPixel(increment)
    }

    repaint();
  }

  /**
   * barIndex -> time
   *
   * @param barIndex, index of bars, start from 1 and to nBars
   * @return time
   */
  final def tb(barIndex: Int): Long = {
    masterSer.timeOfRow(rb(barIndex))
  }

  final def rb(barIndex: Int): Int = {
    /** when barIndex equals it's max: nBars, row should equals rightTimeRow */
    getController.getRightSideRow - nBars + barIndex
  }

  /**
   * time -> barIndex
   *
   * @param time
   * @return index of bars, start from 1 and to nBars
   */
  final def bt(time: Long): Int = {
    br(masterSer.rowOfTime(time))
  }

  final def br(row: Int): Int = {
    row - getController.getRightSideRow + nBars
  }

  def getMaxValue: Float = {
    maxValue
  }

  def getMinValue: Float = {
    minValue
  }

  final def getNBars: Int = {
    nBars
  }

  def getGlassPane = {
    glassPane
  }

  def getMainChartPane: ChartPane = {
    mainChartPane
  }

  def getAxisXPane: AxisXPane = {
    axisXPane
  }

  def getAxisYPane: AxisYPane = {
    axisYPane
  }

  final def getController: ChartingController = {
    controller
  }

  final def getMainSer: TSer = {
    mainSer
  }

  def getMainLayeredPane: JLayeredPane = {
    mainLayeredPane
  }

  def getMainSerChartMapVars = {
    mainSerChartMapVars
  }

  def getChartMapVars(ser: TSer): LinkedHashMap[Chart, HashSet[TVar[_]]] = {
    assert(ser != null, "Do not pass me a null ser!")
    if (ser == getMainSer) mainSerChartMapVars else overlappingSerChartMapVars.get(ser).get
  }

  def getOverlappingSers = {
    overlappingSerChartMapVars.keySet
  }

  def getAllSers = {
    val allSers = new HashSet[TSer]

    allSers += getMainSer
    allSers ++= getOverlappingSers

    allSers
  }

  def popupToDesktop {
  }

  def addOverlappingCharts(ser: TSer) {
    ser.addSerChangeListener(serChangeListener)

    val chartVarsMap = new LinkedHashMap[Chart, HashSet[TVar[_]]]
    overlappingSerChartMapVars += (ser -> chartVarsMap)

    var depthGradient = Pane.DEPTH_GRADIENT_BEGIN;

    for (v <- ser.vars) {
      val chartVars = new HashSet[TVar[_]]
      val chart = ChartFactory.createVarChart(chartVars, v)
      if (chart != null) {
        chartVarsMap.put(chart, chartVars)

        chart.set(mainChartPane, ser)

        chart match {
          case _: GradientChart => chart.setDepth(depthGradient); depthGradient -= 1
          case _: ProfileChart =>  chart.setDepth(depthGradient); depthGradient -= 1
          case _: StickChart => chart.setDepth(-8)
          case _ => chart.setDepth(lastDepthOfOverlappingChart); lastDepthOfOverlappingChart += 1
        }

        mainChartPane.putChart(chart)
      }
    }

    notifyObserversChanged(classOf[ChartValidityObserver[Any]])

    repaint();
  }

  def removeOverlappingCharts(ser: TSer) {
    ser.removeSerChangeListener(serChangeListener)

    overlappingSerChartMapVars.get(ser) foreach {chartVarsMap =>
      for (chart <- chartVarsMap.keySet) {
        mainChartPane.removeChart(chart)
        chart match {
          case _: GradientChart => /** noop */
          case _: ProfileChart => /** noop */
          case _: StickChart => /** noop */
          case _ => lastDepthOfOverlappingChart -= 1
        }
      }
      /** release chartVarsMap */
      chartVarsMap.clear
      overlappingSerChartMapVars.remove(ser)
    }

    notifyObserversChanged(classOf[ChartValidityObserver[Any]])

    repaint();
  }

  def computeMaxMin {
    /** if don't need maxValue/minValue, don't let them all equal 0, just set them to 1 and 0 */
    maxValue = 1
    minValue = 0
  }

  protected def putChartsOfMainSer: Unit

  /** this method only process FinishedComputing event, if you want more, do it in subclass */
  protected def updateView(evt: SerChangeEvent) {
    if (evt.tpe == SerChangeEvent.Type.FinishedComputing) {
      if (this.isInstanceOf[WithDrawingPane]) {
        val drawing = ChartView.this.asInstanceOf[WithDrawingPane].getSelectedDrawing
        if (drawing != null && drawing.isInDrawing) {
          return;
        }
      }

      notifyObserversChanged(classOf[ChartValidityObserver[Any]])

      /** repaint this chart view */
      repaint()
    }
  }

  /**
   * @return x-control pane, may be <code>null</code>
   */
  def getXControlPane: XControlPane = {
    xControlPane
  }

  /**
   * @return y-control pane, may be <code>null</code>
   */
  def getYControlPane: YControlPane = {
    yControlPane
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    if (serChangeListener != null) {
      mainSer.removeSerChangeListener(serChangeListener)
    }

    super.finalize
  }

  class MySerChangeListener extends SerChangeListener {

    def serChanged(evt: SerChangeEvent) {
      evt.tpe match {
        case SerChangeEvent.Type.FinishedComputing | SerChangeEvent.Type.Updated =>
          updateView(evt)
        case _ =>
      }

      /** precess event's call back */
      evt.callBack
    }
  }
}



