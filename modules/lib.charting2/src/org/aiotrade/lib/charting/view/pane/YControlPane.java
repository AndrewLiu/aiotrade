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

import java.awt.BorderLayout;
import org.aiotrade.lib.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartView;
import org.aiotrade.lib.util.ReferenceOnly;

/**
 *
 * @author Caoyuan Deng
 */
public class YControlPane extends Pane {
    MyScrollControl scrollControl;
    
    public YControlPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);
        
        setOpaque(false);
        setRenderStrategy(RenderStrategy.NoneBuffer);
        
        scrollControl = new MyScrollControl();
        scrollControl.setExtendable(false);
        scrollControl.setScalable(false);

        setLayout(new BorderLayout());
        add(scrollControl, BorderLayout.CENTER);
    }
    
    public void setAutoHidden(boolean b) {
        scrollControl.setAutoHidden(b);
    }
    
    public void syncWithView() {
        ChartPane mainChartPane = view.getMainChartPane();
        
        double yChartScale = mainChartPane.getYChartScale();
        
        double vModelRange = 1.0;
        double modelEnd = 1.0;
        double vShownRange = 0.2;
        double vShownEnd = yChartScale;
        
        double unit = 0.05f;
        int nUnitsBlock = 3;
        
        scrollControl.setValues(vModelRange, vShownRange, modelEnd, vShownEnd, unit, nUnitsBlock);
        
        boolean autoHidden = LookFeel.getCurrent().isAutoHideScroll();
        scrollControl.setAutoHidden(autoHidden);
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
    
    private class MyScrollControl extends AbstractScrollControl {
        protected void viewScrolledByUnit(double nUnitsWithDirection) {
            float yChartScale = (float)scrollControl.getValueShownEnd();
            
            view.setYChartScale(yChartScale);
        }
        
        protected void viewScaledToRange(double viewRange) {
        }
    }
    
    @ReferenceOnly public void syncWithView_scrollChart() {
        ChartPane mainChartPane = view.getMainChartPane();
        
        double hCanvas = mainChartPane.getHCanvas();
        double yCanvasCenter = mainChartPane.getYCanvasUpper() + hCanvas * 0.5;
        
        /** define the modelRange, as the value range of chart is relative fixed, so: */
        
        double chartValueBeg = mainChartPane.getMinValue();
        double chartValueEnd = mainChartPane.getMaxValue();
        double chartValueRange = chartValueEnd - chartValueBeg;
        /** give 8 times space for scrolling */
        double modelValueRange = chartValueRange * 8.0;
        double modelRange = modelValueRange;
        
        /** now try to find the modelBeg and modelEnd, we can decide the middle is at canvas center: */
        double modelCenter = mainChartPane.vy((float)yCanvasCenter);
        double modelEnd = modelCenter + modelRange * 0.5;
        double modelBeg = modelEnd - modelRange;
        
        double canvasValueBeg = mainChartPane.vy((float)mainChartPane.getYChartLower());
        double canvasValueEnd = mainChartPane.vy((float)mainChartPane.getYChartUpper());
        double canvasValueRange = canvasValueEnd - canvasValueBeg;
        
        double viewRange = canvasValueRange;
        double viewEnd = canvasValueEnd;
        
        /** the unit here is value-per-pixels, so when 1 UNIT is moved, will scroll unit value on pane */
        double unit = 1.0 / mainChartPane.getHOne();
        int blockUnits = (int)(hCanvas * 0.168 / mainChartPane.getHOne());
        
        scrollControl.setValues(modelRange, viewRange, modelEnd, viewEnd, unit, blockUnits);
    }
    
    @ReferenceOnly private class MyScrollControl_scrollChart extends AbstractScrollControl {
        ChartPane mainChartPane = view.getMainChartPane();
        
        protected void viewScrolledByUnit(double nUnitsWithDirection) {
            view.scrollChartsVerticallyByPixel((int)nUnitsWithDirection);
        }
        
        protected void viewScaledToRange(double viewRange) {
            view.setYChartScaleByCanvasValueRange(viewRange);
        }
    }
    
}


