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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Stroke
import java.awt.geom.GeneralPath
import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.math.timeseries.MasterTSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.charting.view.pane.DatumPlane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.AbstractWidget
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.util.collection.ArrayList


/**
 *
 * @author Caoyuan Deng
 */
object AbstractChart {
  val MARK_INTERVAL = 16

  private val COLOR_SELECTED = new Color(0x447BCD)
  private val COLOR_HIGHLIGHTED = COLOR_SELECTED.darker
  private val COLOR_HOVERED = COLOR_SELECTED.brighter

  private val BASE_STROKES = Array[Stroke](
    new BasicStroke(1.0f),
    new BasicStroke(2.0f)
  )
  private val DASH_PATTERN = Array[Float](5, 2)
  private val DASH_STROKES = Array[Stroke](
    new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0),
    new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0)
  )

  /**
   * To allow the mouse pick up accurately a chart, we need seperate a chart to
   * a lot of segment, each segment is a shape that could be sensible for the
   * mouse row. The minimum segment's width is defined here.
   *
   * Although we can define it > 1, such as 3 or 5, but, when 2 bars or more
   * are located in the same one segment, they can have only one color,
   * example: two-colors candle chart. So, we just simplely define it as 1.
   *
   * Another solution is define 1 n-colors chart as n 1-color charts (implemented).
   */
  private val MIN_SEGMENT_WIDTH = 1
}

abstract class AbstractChart extends AbstractWidget with Chart {
  import AbstractChart._
  import Chart._

  private val markPoints = new ArrayList[Point] // used to draw selected mark
    
  /** Component that charts x-y based on */
  protected var datumPlane: DatumPlane = _
    
  /** masterSer that will be got from: datumPlane.getMasterSer() */
  protected var masterSer: MasterTSer = _
    
  protected var ser: TSer = _
    
  /**
   * the depth of this chart in pane,
   * the chart's container will decide the chart's defaultColor according to the depth
   */
  private var depth: Int = _
    
  private var strockWidth = 1.0f
  private var strockType: StrockType = StrockType.Base
    
  /**
   * Keep convenient references to datumPane's geometry, thus we can also
   * shield the changes from datumPane.
   */
  protected var nBars: Int = _
  protected var wBar: Float = _
    

  protected var wSeg = MIN_SEGMENT_WIDTH
  protected var nSegs: Int = _
    
  protected var nBarsCompressed = 1
    
  private var selected: Boolean = _
  private var firstPlotting: Boolean = _
    
  /** @TODO */
  override def isContainerOnly: Boolean = {
    true
  }
    
  /**
   * NOTICE
   * It's always better to set datumPlane here.
   * After call following set(,,,) methods, the chart can be put in the any
   * pane that has this datumPlane referenced by pane.putChart() for
   * automatical drawing, or, can be drawn on these pane by call pane.render(g)
   * initiatively (such as mouse cursor chart).
   * So, do not try to separate a setPane(Pane) method.
   */
  def set(datumPane: DatumPlane, ser: TSer, depth: Int) {
    this.datumPlane = datumPane
    this.ser = ser
    this.depth = depth
  }
    
  def set(datumPane: DatumPlane, ser: TSer) {
    set(datumPane, ser, this.depth)
  }
    
  def setFirstPlotting(b: Boolean) {
    this.firstPlotting = b
  }
    
  def isFirstPlotting: Boolean = {
    firstPlotting
  }
    
  /**
   * present only prepare the chart's pathSegs and textSegs, but not really render,
   * should call render(Graphics2D g) to render this chart upon g
   */
  protected def plotWidget {
    this.masterSer = datumPlane.getMasterSer
    this.nBars     = datumPlane.getNBars
    this.wBar      = datumPlane.getWBar
        
    this.wSeg = Math.max(wBar, MIN_SEGMENT_WIDTH).intValue
    this.nSegs = (nBars * wBar / wSeg).intValue + 1
        
    this.nBarsCompressed = if (wBar >= 1) 1 else (1 / wBar).intValue
        
    reset
        
    plotChart
  }
    
  protected def plotChart: Unit
    
  override def reset {
    super.reset

    markPoints.clear
  }
    
  protected def addMarkPoint(x: Int, y: Int) {
    markPoints+= new Point(x, y)
  }
    
  /**
   * use intersects instead of contains here, contains means:
   * A specified coordinate is inside the boundary of this chart.
   * But not each kind of chart has boundary.
   * For example: in the case of Line, objects it always contains nothing,
   * since a line contains no area.
   */
  override protected def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    false
  }
    
  protected def renderWidget(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]
        
    val w = getStrockWidth.intValue
    var stroke: Stroke = getStrockType match {
      case StrockType.Base =>
        if (w <= BASE_STROKES.length) BASE_STROKES(w - 1)
        else new BasicStroke(w)
      case StrockType.Dash =>
        if (w <= DASH_STROKES.length) DASH_STROKES(w - 1)
        else new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0)
      case _ => new BasicStroke(w)
    }
    g.setStroke(stroke)
        
    if (isSelected) {
      for (point <- markPoints) {
        renderMarkAtPoint(g, point)
      }
    }
  }
    
  private def renderMarkAtPoint(g: Graphics, point: Point) {
    g.setColor(LookFeel.getCurrent.handleColor)
    g.fillRect(point.x - 2, point.y - 2, 5, 5)
  }
    
  def setDepth(depth: Int) {
    this.depth = depth
  }
    
  def getDepth: Int = {
    depth
  }
    
  def isSelected: Boolean = {
    selected
  }
    
  def setSelected(b: Boolean) {
    this.selected = b
  }
    
  def setStrock(width: Int, tpe: StrockType) {
    this.strockWidth = width
    this.strockType = tpe
  }
    
  /**
   * @return width of chart. not width of canvas!
   */
  def getStrockWidth: Float = {
    strockWidth
  }
    
  def getStrockType: StrockType = {
    strockType
  }
    
  def setSer(ser: TSer) {
    this.ser = ser
  }
    
  def getSer: TSer = {
    ser
  }
    
  /**
   * Translate barIndex to X point for drawing
   *
   * @param barIndex: index of bars, start from 1 to nBars
   */
  final protected def xb(barIndex: Int): Float = {
    this.datumPlane.xb(barIndex)
  }

  /**
   * Translate value to Y point for drawing
   * @param value
   */
  final protected def yv(value: Float): Float = {
    this.datumPlane.yv(value)
  }

  final protected def bx(x: Float): Int = {
    this.datumPlane.bx(x)
  }

  final protected def vy(y: Float): Float = {
    this.datumPlane.vy(y)
  }

  /**
   * @return row in ser corresponding to barIndex
   */
  final protected def rb(barIndex: Int): Int = {
    this.datumPlane.rb(barIndex)
  }

  final protected def br(row: Int): Int = {
    this.datumPlane.br(row)
  }

  /**
   * @return segment index corresponding to barIdx
   */
  final protected def sb(barIdx: Int): Int = {
    (barIdx * wBar / wSeg).intValue + 1
  }

  /* final */ protected def bs(segIdx: Int): Int = {
    (((segIdx - 1) * wSeg) / wBar).intValue
  }

  /**
   * @param barIdx: index of bars, start from 1 to nBars
   */
  final protected def tb(barIdx: Int): Long = {
    this.datumPlane.tb(barIdx)
  }

  final protected def bt(time: Long): Int = {
    this.datumPlane.bt(time)
  }

  protected def plotLine(xBase: Float, yBase: Float, k: Float,  path: GeneralPath) {
    val xBeg = 0
    val yBeg = GeomUtil.yOfLine(xBeg, xBase, yBase, k)
    val xEnd = datumPlane.getWidth
    val yEnd = GeomUtil.yOfLine(xEnd, xBase, yBase, k)
    path.moveTo(xBeg, yBeg)
    path.lineTo(xEnd, yEnd)
  }

  protected def plotVerticalLine(bar: Int, path: GeneralPath) {
    val x = xb(bar);
    val yBeg = datumPlane.getYCanvasLower
    val yEnd = datumPlane.getYCanvasUpper
    path.moveTo(x, yBeg)
    path.lineTo(x, yEnd)
  }

  protected def plotLineSegment(xBeg: Float, yBeg: Float, xEnd: Float, yEnd: Float, path: GeneralPath) {
    path.moveTo(xBeg, yBeg)
    path.lineTo(xEnd, yEnd)
  }

  protected def plotVerticalLineSegment(bar: Int, yBeg: Float, yEnd: Float, path: GeneralPath) {
    val x = xb(bar)
    path.moveTo(x, yBeg)
    path.lineTo(x, yEnd)
  }

  /** compare according to the depth of chart, used for SortedSet<Chart> */
  final def compare(another: Chart): Int = {
    if (this.getDepth == another.getDepth) {
      if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
    } else {
      if (this.getDepth < another.getDepth) -1 else 1
    }
  }

    
  /**
   * @ReferenceOnly methods:
   * ----------------------------------------------------------------
   */
    
  /**
   * @deprecated
   */
  @deprecated private def plotLine_seg(xCenter: Float, yCenter: Float, k: Float, path: GeneralPath) {
    var xlast = xb(0) // bar 0
    var ylast = Null.Float
    var bar = 1
    while (bar <= nBars) {
            
      var x1 = xlast
      var y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k)
            
      var x2 = xb(bar)
      var y2 = GeomUtil.yOfLine(x2, xCenter, yCenter, k)
            
      /**
       * if (xlast, y1) is the same point of (xlast, ylast), let
       *     x1 = xlast + 1
       * to avoid the 1 point intersect at the each path's
       * end point, especially in XOR mode:
       */
      var break = false
      while (x1 < x2 && !break) {
        if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
          x1 +=1
          y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k)
        } else {
          break = true
        }
      }
            
      path.moveTo(x1, y1)
      path.lineTo(x2, y2)
            
      ylast = y2
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotLineSegment_seg(xBeg: Float, yBeg: Float, xEnd: Float, yEnd: Float, path: GeneralPath) {
    val dx = xEnd - xBeg
    val dy = yEnd - yBeg
        
    val k: Float = if (dx == 0) 1 else dy / dx
    val xmin = Math.min(xBeg, xEnd)
    val xmax = Math.max(xBeg, xEnd)
    val ymin = Math.min(yBeg, yEnd)
    val ymax = Math.max(yBeg, yEnd)
        
    var xlast = xb(0) // bar 0
    var ylast = Null.Float
    var bar = 1
    while (bar <= nBars) {
            
      var x1 = xlast
      var x2 = xb(bar)
            
      var y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k)
      var y2 = GeomUtil.yOfLine(x2, xBeg, yBeg, k)
            
            
      if (x1 >= xmin && x1 <= xmax && x2 >= xmin && x2 <= xmax &&
          y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax
      ) {
                
        /**
         * if (xlast, y1) is the same point of (xlast, ylast), let
         *     x1 = xlast + 1
         * to avoid the 1 point intersect at the each path's
         * end point, especially in XOR mode:
         */
        var break = false
        while (x1 < x2 && !break) {
          if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
            x1 += 1
            y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k)
          } else {
            break = true
          }
        }
                
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
                
        ylast = y2
                
      }
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotVerticalLine_seg(bar: Int, path: GeneralPath) {
    if (bar >= 1 && bar <= nBars) {
            
      val y1 = yv(datumPlane.getMinValue)
      val y2 = yv(datumPlane.getMinValue)
      val x = xb(bar)
            
      path.moveTo(x, y1)
      path.lineTo(x, y2)
            
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotVerticalLineSegment_seg(bar: Int, yBeg: Float, yEnd: Float, path: GeneralPath) {
    if (bar >= 1 && bar <= nBars) {
            
      val x = xb(bar)
            
      path.moveTo(x, yBeg)
      path.lineTo(x, yEnd)
            
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotArc_seg(xCenter: Float, yCenter: Float, radius: Double, path: GeneralPath) {
    plotHalfArc_seg(xCenter, yCenter, radius, true, path)
    plotHalfArc_seg(xCenter, yCenter, radius, false, path)
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotHalfArc_seg(xCenter: Float, yCenter: Float, radius: Double, positiveSide: Boolean, path: GeneralPath) {
    var xlast = xb(0) // bar 0
    var ylast = Null.Float
    var bar = 1
    while (bar <= nBars) {
            
      val x1 = xlast
      val x2 = xb(bar)
            
      /** draw positive arc from x1 to x2 */
      val y1 = GeomUtil.yOfCircle(x1, xCenter, yCenter, radius, positiveSide)
            
      /**
       * if (xlast, y1) is the same point of (xlast, ylast), let
       *     x1 = xlast + 1
       * to avoid the 1 point intersect at the each path's
       * end point, especially in XOR mode:
       *
       * In case of: step = (xfactor <= 2) ? 3 : 1, following code could be ignored:
       *
       * if (isTheSamePoint(x1, y1, xlast, ylast)) {
       *     x1 = xlast + 1;
       *     y1 = yOfArc(x1, xCenter, yCenter, radius, positiveSide);
       * }
       *
       */
            
            
      if (y1 != Null.Float) {
        path.moveTo(x1, y1)

        var x = x1 + 1
        while (x <= x2) {
          val y =  GeomUtil.yOfCircle(x, xCenter, yCenter, radius, positiveSide)
                    
          if (y != Null.Float) {
            path.lineTo(x, y)
                        
            ylast = y
          }
          x +=1
        }
      }
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
    
}