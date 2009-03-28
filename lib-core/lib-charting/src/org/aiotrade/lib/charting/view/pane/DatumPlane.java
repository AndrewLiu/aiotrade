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
package org.aiotrade.lib.charting.view.pane;

import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.charting.view.scalar.Scalar;
import org.aiotrade.lib.math.timeseries.MasterSer;


/**
 * This is the only Type aware of x-y coordinate and knows how to compute it. 
 * At this time, only ChartPane implements it, other panes should have a referrence
 * DatumPlane as its member, and get the coordinate information from it.
 *
 * @author Caoyuan Deng
 */
public interface DatumPlane {
    
    int getWidth();
    int getHeight();
    
    boolean isMouseEntered();
    int getYMouse();
    float getReferCursorValue();
    boolean isAutoReferCursorValue();
    
    ChartView getView();
    
    MasterSer getMasterSer();
    
    /** 
     * in DatumPlane, we define this public interface for its users to call in case
     * of being painted earlier than DatumPlane, for example: AxisXPane and AxisYPane.
     * @see Pane#prePaintComponent()
     */
    void computeGeometry();
    boolean isGeometryValid();
    
    int getNBars();
    
    float getWBar();
    
    float getHOne();
    
    float getMaxValue();
    
    float getMinValue();
    
    float xb(int barIndex);
    int bx(float x);
    
    float xr(int row);
    int rx(float x);
    
    float yv(float value);
    float vy(float y);
    
    int rb(int barIndex);
    int br(int row);
    
    long tb(int barIdx);
    int bt(long time);
    
    long tx(float x);
        
    int getHCanvas();
    
    int getYCanvasLower();
    
    int getYCanvasUpper();
    
    /**
     * @return chart height in pixels, corresponds to the value range (maxValue - minValue)
     */
    int getHChart();
    
    int getYChartLower();
    
    int getYChartUpper();
    
    Scalar getValueScalar();
    
    void setValueScalar(Scalar valueScalar);
    
    float getYChartScale();
    
    void setYChartScale(float yChartScale);
    
    void growYChartScale(float increment);
    
    void setYChartScaleByCanvasValueRange(double canvasValueRange);
    
    void scrollChartsVerticallyByPixel(int increment);
    
}






