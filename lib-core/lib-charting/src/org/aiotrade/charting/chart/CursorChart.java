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
package org.aiotrade.charting.chart;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import org.aiotrade.charting.widget.PathWidget;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.charting.laf.LookFeel;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class CursorChart extends AbstractChart<WidgetModel> {
    
    protected static DecimalFormat MONEY_DECIMAL_FORMAT = new DecimalFormat("0.###");
    protected static DecimalFormat STOCK_DECIMAL_FORMAT = new DecimalFormat("0.00");
    
    protected LookFeel laf;
    protected Color fgColor;
    protected Color bgColor;
    
    protected int referRow;
    protected int mouseRow;
    protected long referTime;
    protected long mouseTime;
    protected float x;
    
    protected GeneralPath cursorPath;
    
    private Type type = Type.Mouse;
    
    public enum Type {
        Refer,
        Mouse;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    protected WidgetModel createModel() {
        return null;
    }
    
    protected void plotChart() {
        laf = LookFeel.getCurrent();
        
        referRow  = datumPlane.getView().getController().getReferCursorRow();
        referTime = datumPlane.getView().getController().getReferCursorTime();
        mouseRow  = datumPlane.getView().getController().getMouseCursorRow();
        mouseTime = datumPlane.getView().getController().getMouseCursorTime();
        
        PathWidget pathWidget = addChild(new PathWidget());
        switch (type) {
            case Refer:
                fgColor = laf.referCursorColor;
                bgColor = laf.referCursorColor;
                pathWidget.setForeground(fgColor);
                
                cursorPath = pathWidget.getPath();
                
                x = xb(br(referRow));
                
                plotReferCursor();
                
                break;
            case Mouse:
                if (! datumPlane.getView().getController().isMouseEnteredAnyChartPane()) {
                    return;
                }
                
                fgColor = laf.mouseCursorColor;
                bgColor = Color.YELLOW;
                pathWidget.setForeground(fgColor);
                
                cursorPath = pathWidget.getPath();
                
                x = xb(br(mouseRow));
                
                plotMouseCursor();
                
                break;
            default:
        }
    }
    
    protected abstract void plotReferCursor();
    
    protected abstract void plotMouseCursor();
    
    /** CursorChart always returns false */
    @Override
    public boolean isSelected() {
        return false;
    }
    
}