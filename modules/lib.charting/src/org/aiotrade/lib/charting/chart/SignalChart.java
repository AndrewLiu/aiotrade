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
package org.aiotrade.lib.charting.chart;

import java.awt.Color;
import org.aiotrade.lib.charting.widget.Arrow;
import org.aiotrade.lib.charting.widget.HeavyPathWidget;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.charting.chart.SignalChart.Model;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.math.util.Signal;

/**
 *
 * @author Caoyuan Deng
 */
public class SignalChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        Var<Signal> var;
        
        public void set(Var<Signal> var) {
            this.var = var;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        final Color positiveColor = LookFeel.getCurrent().getPositiveColor();
        final Color negativeColor = LookFeel.getCurrent().getNegativeColor();
        
        Color color = Color.YELLOW;
        setForeground(color);
        
        final HeavyPathWidget heavyPathWidget = addChild(new HeavyPathWidget());
        final Arrow template = new Arrow();
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            for (int i = 0; i < nBarsCompressed; i++) {
                final long time = tb(bar + i);
                
                final Signal signal = model.var.getByTime(time);
                if (signal != null) {
                    final float value = signal.getValue();
                    
                    if (! Float.isNaN(value)) {
                        float x = xb(bar);
                        float y = yv(value);
                        
                        switch (signal.getSign()) {
                            case EnterLong:
                                template.setForeground(color);
                                template.model().set(x, y + 3, true, false);
                                break;
                            case ExitLong:
                                template.setForeground(color);
                                template.model().set(x, y - 3, false, false);
                                break;
                            case EnterShort:
                                template.setForeground(color);
                                template.model().set(x, y + 3, false, false);
                                break;
                            case ExitShort:
                                template.setForeground(color);
                                template.model().set(x, y - 3, true, false);
                                break;
                            default:
                        }
                        template.plot();
                        heavyPathWidget.appendFrom(template);
                    }
                }
            }
        }
        
    }
    
}



