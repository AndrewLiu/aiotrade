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
package org.aiotrade.lib.charting.view.pane

import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.WithVolumePane
import org.aiotrade.lib.charting.view.scalar.LinearScalar
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.math.timeseries.MasterTSer

/**
 *
 * @author Caoyuan Deng
 * 
 * @todo the ChangeObservable should notify according to priority
 *
 * call super(view, null) will let the super know this pane will be its
 * own datumPlane.
 * @see Pane#Pane(ChartView, DatumPlane)
 */
 abstract class AbstractDatumPlane(view: ChartView) extends Pane(view, null) with DatumPlane {
    
    private var geometryValid: Boolean = _
    
    /** geometry that need to be set before chart plotting and render */
    private var nBars: Int = _ // fetched from view, number of bars, you may consider it as chart width
    private var hChart: Int = _ // chart height in pixels, corresponds to the value range (maxValue - minValue)
    private var hCanvas: Int = _ // canvas height in pixels
    private var hChartOffsetToCanvas: Int = _ // chart's axis-y offset in canvas, named hXXXX means positive is from lower to upper;
    private var hSpaceLower: Int = _ // height of spare space at lower side
    private var hSpaceUpper: Int = _ // height of spare space at upper side
    private var yCanvasLower: Int = _ // y of canvas' lower side
    private var yChartLower: Int = _ // y of chart's lower side
    private var wBar: Float = _ // fetched from viewContainer, pixels per bar
    private var hOne: Float = _ // pixels per 1.0 value
    private var maxValue: Float = _ // fetched from view
    private var minValue: Float = _ // fetched from view
    private var maxScaledValue: Float = _
    private var minScaledValue: Float = _
    
    private var valueScalar: Scalar = new LinearScalar
    
    /**
     * the percent of hCanvas to be used to render charty, is can be used to scale the chart
     */
    private var yChartScale = 1.0f
    
    /** the pixels used to record the chart vertically moving */
    private var hChartScrolled: Int = _
        
    def computeGeometry {
      this.wBar  = view.getController.getWBar
      this.nBars = view.getNBars
        
      /**
       * @TIPS:
       * if want to leave spare space at lower side, do hCanvas -= space
       * if want to leave spare space at upper side, do hChart = hCanvas - space
       *     hOne = hChart / (maxValue - minValue)
       */
      hSpaceLower = 1
      if (view.getXControlPane != null) {
        /** leave xControlPane's space at lower side */
        hSpaceLower += view.getXControlPane.getHeight
      }
        
      /** default values: */
      hSpaceUpper = 0
      maxValue = view.getMaxValue
      minValue = view.getMinValue
        
      /** adjust if necessary */
      if (this.equals(view.getMainChartPane)) {
        hSpaceUpper += ChartView.TITLE_HEIGHT_PER_LINE
      } else if (view.isInstanceOf[WithVolumePane] && this.equals(view.asInstanceOf[WithVolumePane].getVolumeChartPane)) {
        maxValue = view.asInstanceOf[WithVolumePane].getMaxVolume
        minValue = view.asInstanceOf[WithVolumePane].getMinVolume
      }
        
      this.maxScaledValue = valueScalar.doScale(maxValue)
      this.minScaledValue = valueScalar.doScale(minValue)
        
      this.hCanvas = getHeight - hSpaceLower - hSpaceUpper
        
      val hChartCouldBe = hCanvas
      this.hChart = (hChartCouldBe * yChartScale).intValue
        
      /** allocate sparePixelsBroughtByYChartScale to upper and lower averagyly */
      val sparePixelsBroughtByYChartScale = hChartCouldBe - hChart
      hChartOffsetToCanvas = hChartScrolled + (sparePixelsBroughtByYChartScale * 0.5).intValue
        
        
      yCanvasLower = hSpaceUpper + hCanvas
      yChartLower = yCanvasLower - hChartOffsetToCanvas
        
      /**
       * @NOTICE
       * the chart height corresponds to value range.
       * (not canvas height, which may contain values exceed max/min)
       */
      hOne = hChart.floatValue / (maxScaledValue - minScaledValue)
        
      /** avoid hOne == 0 */
      this.hOne = Math.max(hOne, 0.0000000001f)
        
      setGeometryValid(true)
    }
    
    def isGeometryValid: Boolean = {
      geometryValid
    }
    
    protected def setGeometryValid(b: Boolean) {
      this.geometryValid = b
    }
    
    def getValueScalar: Scalar = {
      valueScalar
    }
    
    def setValueScalar(valueScalar: Scalar) {
      this.valueScalar = valueScalar
    }
    
    def getYChartScale: Float = {
      yChartScale
    }
    
    def setYChartScale(yChartScale: Float) {
      val oldValue = this.yChartScale
      this.yChartScale = yChartScale
        
      if (oldValue != this.yChartScale) {
        setGeometryValid(false)
        repaint()
      }
    }
    
    def growYChartScale(increment: Float) {
      setYChartScale(getYChartScale + increment)
    }
    
    def setYChartScaleByCanvasValueRange(canvasValueRange: Double) {
      val oldCanvasValueRange = vy(getYCanvasUpper) - vy(getYCanvasLower)
      val scale = oldCanvasValueRange / canvasValueRange.floatValue
      val newYChartScale = yChartScale * scale
        
      setYChartScale(newYChartScale)
    }
    
    def scrollChartsVerticallyByPixel(increment: Int) {
      hChartScrolled += increment
        
      /** let repaint() to update the hChartOffsetToCanvas and other geom */
      repaint()
    }
    
    def getMasterSer: MasterTSer = {
      view.getController.getMasterSer
    }
    
    /**
     * barIndex -> x
     *
     * @param i index of bars, start from 1 to nBars
     * @return x
     */
    final def xb(barIndex: Int): Float = {
      wBar * (barIndex - 1)
    }
    
    final def xr(row: Int): Float = {
      xb(br(row))
    }
    
    /**
     * y <- value
     *
     * @param value
     * @return y on the pane
     */
    final def yv(value: Float): Float = {
      val scaledValue = valueScalar.doScale(value)
      GeomUtil.yv(scaledValue, hOne, minScaledValue, yChartLower)
    }
    
    /**
     * value <- y
     * @param y y on the pane
     * @return value
     */
    final def vy(y: Float): Float = {
      val scaledValue = GeomUtil.vy(y, hOne, minScaledValue, yChartLower)
      valueScalar.unScale(scaledValue)
    }
    
    /**
     * barIndex <- x
     *
     * @param x x on the pane
     * @return index of bars, start from 1 to nBars
     */
    final def bx(x: Float): Int = {
      Math.round(x / wBar + 1)
    }
    
    
    /**
     * time <- x
     */
    final def tx(x: Float): Long = {
      tb(bx(x))
    }

    /** row <- x */
    final def rx(x: Float): Int = {
      rb(bx(x));
    }
    
    final def rb(barIndex: Int): Int = {
      /** when barIndex equals it's max: nBars, row should equals rightTimeRow */
      view.getController.getRightSideRow - nBars + barIndex
    }
    
    final def br(row: Int): Int = {
      row - view.getController.getRightSideRow + nBars
    }
    
    /**
     * barIndex -> time
     *
     * @param barIndex, index of bars, start from 1 and to nBars
     * @return time
     */
    final def tb(barIndex: Int): Long = {
      view.getController.getMasterSer.timeOfRow(rb(barIndex))
    }
    
    /**
     * time -> barIndex
     *
     * @param time
     * @return index of bars, start from 1 and to nBars
     */
    final def bt(time: Long): Int = {
      br(view.getController.getMasterSer.rowOfTime(time))
    }
    
    def getNBars: Int = {
      nBars
    }
    
    def getWBar: Float = {
      wBar
    }
    
    /**
     * @return height of 1.0 value in pixels
     */
    def getHOne: Float = {
      hOne
    }
    
    def getHCanvas: Int = {
      hCanvas
    }
    
    def getYCanvasLower: Int = {
      yCanvasLower
    }
    
    def getYCanvasUpper: Int = {
      hSpaceUpper
    }
    
    /**
     * @return chart height in pixels, corresponds to the value range (maxValue - minValue)
     */
    def getHChart: Int = {
      hChart
    }
    
    def getYChartLower: Int = {
      yChartLower
    }
    
    def getYChartUpper: Int = {
      getYChartLower - hChart
    }
    
    def getMaxValue: Float = {
      maxValue
    }
    
    def getMinValue: Float = {
      minValue
    }

    @throws(classOf[Throwable])
    override protected def finalize {
      view.getController.removeObserversOf(this)
      view.removeObserversOf(this)

      super.finalize
    }
    
  }
