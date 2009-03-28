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
import java.util.Calendar;
import org.aiotrade.charting.widget.HeavyPathWidget;
import org.aiotrade.charting.widget.PathWidget;
import org.aiotrade.charting.widget.WidgetModel;




/**
 *
 * @author Caoyuan Deng
 */
public class ChineseMonthChart extends AbstractChart<WidgetModel> {
    
    protected WidgetModel createModel() {
        return null;
    }
    
    protected void plotChart() {
        final Calendar cal = Calendar.getInstance();
        Color color = Color.RED.darker();
        setForeground(color);
        
        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final PathWidget template = new PathWidget();
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            int chineseMonth = -1;
            for (int i = 0; i < nBarsCompressed; i++) {
                long time = tb(bar + i);
                
                cal.setTimeInMillis(time);
                chineseMonth = -1; //cal.getChineseMonth();
            }
            
            if (chineseMonth > 0) {
                template.setForeground(color);
                final GeneralPath path = template.getPath();
                path.moveTo(xb(bar), yv(datumPlane.getMinValue()));
                path.lineTo(xb(bar), yv(datumPlane.getMaxValue()));
                heavyPathWidget.appendFrom(template);
            }
            
        }
        
    }
}
