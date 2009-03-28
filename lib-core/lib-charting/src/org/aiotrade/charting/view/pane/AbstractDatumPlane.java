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
package org.aiotrade.charting.view.pane;

import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.WithVolumePane;
import org.aiotrade.charting.view.scalar.LinearScalar;
import org.aiotrade.charting.view.scalar.Scalar;
import org.aiotrade.charting.util.GeomUtil;
import org.aiotrade.math.timeseries.MasterSer;

/**
 *
 * @author Caoyuan Deng
 * 
 * @todo the ChangeObservable should notify according to priority
 */
public abstract class AbstractDatumPlane extends Pane implements DatumPlane {
    
    private boolean geometryValid;
    
    /** geometry that need to be set before chart plotting and render */
    private int nBars; // fetched from view, number of bars, you may consider it as chart width
    private int hChart; // chart height in pixels, corresponds to the value range (maxValue - minValue)
    private int hCanvas; // canvas height in pixels
    private int hChartOffsetToCanvas; // chart's axis-y offset in canvas, named hXXXX means positive is from lower to upper;
    private int hSpaceLower; // height of spare space at lower side
    private int hSpaceUpper; // height of spare space at upper side
    private int yCanvasLower; // y of canvas' lower side
    private int yChartLower; // y of chart's lower side
    private float wBar; // fetched from viewContainer, pixels per bar
    private float hOne; // pixels per 1.0 value
    private float maxValue; // fetched from view
    private float minValue; // fetched from view
    private float maxScaledValue;
    private float minScaledValue;
    
    private Scalar valueScalar = new LinearScalar();
    
    /**
     * the percent of hCanvas to be used to render charty, is can be used to scale the chart
     */
    private float yChartScale = 1.0f;
    
    /** the pixels used to record the chart vertically moving */
    private int hChartScrolled;
    
    public AbstractDatumPlane(ChartView view) {
        /**
         * call super(view, null) will let the super know this pane will be its
         * own datumPlane.
         * @see Pane#Pane(ChartView, DatumPlane)
         */
        super(view, null);
    }
    
    public void computeGeometry() {
        this.wBar  = view.getController().getWBar();
        this.nBars = view.getNBars();
        
        /**
         * @TIPS:
         * if want to leave spare space at lower side, do hCanvas -= space
         * if want to leave spare space at upper side, do hChart = hCanvas - space
         *     hOne = hChart / (maxValue - minValue)
         */
        hSpaceLower = 1;
        if (view.getXControlPane() != null) {
            /** leave xControlPane's space at lower side */
            hSpaceLower += view.getXControlPane().getHeight();
        }
        
        /** default values: */
        hSpaceUpper = 0;
        maxValue = view.getMaxValue();
        minValue = view.getMinValue();
        
        /** adjust if necessary */
        if (this.equals(view.getMainChartPane())) {
            hSpaceUpper += ChartView.TITLE_HEIGHT_PER_LINE;
        } else if (view instanceof WithVolumePane && this.equals(((WithVolumePane)view).getVolumeChartPane())) {
            maxValue = ((WithVolumePane)view).getMaxVolume();
            minValue = ((WithVolumePane)view).getMinVolume();
        }
        
        this.maxScaledValue = valueScalar.doScale(maxValue);
        this.minScaledValue = valueScalar.doScale(minValue);
        
        this.hCanvas = getHeight() - hSpaceLower - hSpaceUpper;
        
        int hChartCouldBe = hCanvas;
        this.hChart = (int)(hChartCouldBe * yChartScale);
        
        /** allocate sparePixelsBroughtByYChartScale to upper and lower averagyly */
        float sparePixelsBroughtByYChartScale = hChartCouldBe - hChart;
        hChartOffsetToCanvas = hChartScrolled + (int)(sparePixelsBroughtByYChartScale * 0.5);
        
        
        yCanvasLower = hSpaceUpper + hCanvas;
        yChartLower = yCanvasLower - hChartOffsetToCanvas;
        
        /**
         * @NOTICE
         * the chart height corresponds to value range.
         * (not canvas height, which may contain values exceed max/min)
         */
        hOne = (float)hChart / (maxScaledValue - minScaledValue);
        
        /** avoid hOne == 0 */
        this.hOne = Math.max(hOne, 0.0000000001f);
        
        setGeometryValid(true);
    }
    
    public boolean isGeometryValid() {
        return geometryValid;
    }
    
    protected void setGeometryValid(boolean b) {
        this.geometryValid = b;
    }
    
    public Scalar getValueScalar() {
        return valueScalar;
    }
    
    public void setValueScalar(Scalar valueScalar) {
        this.valueScalar = valueScalar;
    }
    
    public float getYChartScale() {
        return yChartScale;
    }
    
    public void setYChartScale(float yChartScale) {
        final float oldValue = this.yChartScale;
        this.yChartScale = yChartScale;
        
        if (oldValue != this.yChartScale) {
            setGeometryValid(false);
            repaint();
        }
    }
    
    public void growYChartScale(float increment) {
        setYChartScale(getYChartScale() + increment);
    }
    
    public void setYChartScaleByCanvasValueRange(double canvasValueRange) {
        float oldCanvasValueRange = vy(getYCanvasUpper()) - vy(getYCanvasLower());
        float scale = oldCanvasValueRange / (float)canvasValueRange;
        float newYChartScale = yChartScale * scale;
        
        setYChartScale(newYChartScale);
    }
    
    public void scrollChartsVerticallyByPixel(int increment) {
        hChartScrolled += increment;
        
        /** let repaint() to update the hChartOffsetToCanvas and other geom */
        repaint();
    }
    
    public MasterSer getMasterSer() {
        return view.getController().getMasterSer();
    }
    
    /**
     * barIndex -> x
     *
     * @param i index of bars, start from 1 to nBars
     * @return x
     */
    public final float xb(int barIndex) {
        return wBar * (barIndex - 1);
    }
    
    public final float xr(int row) {
        return xb(br(row));
    }
    
    /**
     * y <- value
     *
     * @param value
     * @return y on the pane
     */
    public final float yv(float value) {
        final float scaledValue = valueScalar.doScale(value);
        return GeomUtil.yv(scaledValue, hOne, minScaledValue, yChartLower);
    }
    
    /**
     * value <- y
     * @param y y on the pane
     * @return value
     */
    public final float vy(float y) {
        final float scaledValue = GeomUtil.vy(y, hOne, minScaledValue, yChartLower);
        return valueScalar.unScale(scaledValue);
    }
    
    /**
     * barIndex <- x
     *
     * @param x x on the pane
     * @return index of bars, start from 1 to nBars
     */
    public final int bx(float x) {
        return Math.round(x / wBar + 1);
    }
    
    
    /**
     * time <- x
     */
    public final long tx(float x) {
        return tb(bx(x));
    }
    
    public final int rx(float x) {
        return rb(bx(x));
    }
    
    public final int rb(int barIndex) {
        /** when barIndex equals it's max: nBars, row should equals rightTimeRow */
        return view.getController().getRightSideRow() - nBars + barIndex;
    }
    
    public final int br(int row) {
        return row - view.getController().getRightSideRow() + nBars;
    }
    
    /**
     * barIndex -> time
     *
     * @param barIndex, index of bars, start from 1 and to nBars
     * @return time
     */
    public final long tb(int barIndex) {
        return view.getController().getMasterSer().timeOfRow(rb(barIndex));
    }
    
    /**
     * time -> barIndex
     *
     * @param time
     * @return index of bars, start from 1 and to nBars
     */
    public final int bt(long time) {
        return br(view.getController().getMasterSer().rowOfTime(time));
    }
    
    public int getNBars() {
        return nBars;
    }
    
    public float getWBar() {
        return wBar;
    }
    
    /**
     * @return height of 1.0 value in pixels
     */
    public float getHOne() {
        return hOne;
    }
    
    public int getHCanvas() {
        return hCanvas;
    }
    
    public int getYCanvasLower() {
        return yCanvasLower;
    }
    
    public int getYCanvasUpper() {
        return hSpaceUpper;
    }
    
    /**
     * @return chart height in pixels, corresponds to the value range (maxValue - minValue)
     */
    public int getHChart() {
        return hChart;
    }
    
    public int getYChartLower() {
        return yChartLower;
    }
    
    public int getYChartUpper() {
        return getYChartLower() - hChart;
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    @Override
    protected void finalize() throws Throwable {
        view.getController().removeObserversOf(this);
        view.removeObserversOf(this);

        super.finalize();
    }
    
}
