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

import java.awt.BorderLayout;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartView;

/**
 *
 * @author Caoyuan Deng
 */
public class XControlPane extends Pane {
    MyScrollControl scrollControl;
    
    public XControlPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);
        
        setOpaque(false);
        setRenderStrategy(RenderStrategy.NoneBuffer);
        
        scrollControl = new MyScrollControl();
        scrollControl.setExtendable(true);
        scrollControl.setScalable(true);

        setLayout(new BorderLayout());
        add(scrollControl, BorderLayout.CENTER);
    }
    
    public void setAlwaysHidden(boolean b) {
        scrollControl.setAutoHidden(b);
    }
    
    public void syncWithView() {
        MasterSer masterSer = view.getController().getMasterSer();
        
        double vModelRange = masterSer.size();
        double vShownRange = view.getNBars();
        double vModelEnd = masterSer.lastOccurredRow();
        double vShownEnd = view.getController().getRightSideRow();
        double unit = 1.0;
        int nUnitsBlock = (int)(vShownRange * 0.168);
        
        scrollControl.setValues(vModelRange, vShownRange, vModelEnd, vShownEnd, unit, nUnitsBlock);
        
        boolean autoHidden = LookFeel.getCurrent().isAutoHideScroll();
        scrollControl.setAutoHidden(autoHidden);
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
    
    private class MyScrollControl extends AbstractScrollControl {
        
        protected void viewScrolledByUnit(double nUnitsWithDirection) {
            view.getController().scrollChartsHorizontallyByBar((int)nUnitsWithDirection);
        }
        
        protected void viewScaledToRange(double valueShownRange) {
            view.getController().setWBarByNBars(getWidth(), (int)valueShownRange);
        }
    }
    
}


