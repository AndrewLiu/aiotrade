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
import java.awt.Graphics;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.math.timeseries.SerItem;
import org.aiotrade.lib.math.timeseries.Var;
import org.aiotrade.lib.charting.chart.GradientChart.Model;
import org.aiotrade.lib.charting.chart.util.Shading;
import org.aiotrade.lib.charting.laf.LookFeel;


/**
 *
 * @author Caoyuan Deng
 */
public class GradientChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        Var var;
        Shading shading;

        public void set(Var var, Shading shading) {
            this.var = var;
            this.shading = shading;
        }
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        /** this chart don't use path, just directly draw on screen */
    }
    
    public void render(Graphics g) {
        final Model model = model();
        
        float lower = model.shading.getLowerBound();
        float upper = model.shading.getUpperBound();
        float step  = model.shading.getNIntervals();
        
        Color color = LookFeel.getCurrent().stickChartColor;
        setForeground(color);
        
        final int radius = wBar < 2 ? 0 : (int)((wBar - 2) / 2);
        
        for (int bar = 1; bar <= nBars; bar++) {
            
            long time = tb(bar);
            SerItem item = ser.getItem(time);
            
            if (item != null) {
                Float[] shades = (Float[])item.get(model.var);
                if (shades != null) {
                    float centre = xb(bar);
                    float prevRange = 0;
                    for (int j = 0; j < shades.length; j++) {
                        float range = j * step + lower;
                        
                        float shade = shades[j];
                        if (! Float.isNaN(shade)) {
                            shade = (float)(Math.pow(shade, 1d / 3d));
                            color = new Color(shade, shade, shade);
                            g.setColor(color);
                            g.fillRect((int)(centre - radius - 1), (int)yv(range), (int)(2 * (radius + 1)), (int)yv(prevRange) - (int)yv(range));
                            //g.drawLine((int)barCentre, (int)yv(prevRange), (int)barCentre, (int)yv(range));
                        }
                        prevRange = range;
                    }
                }
            }
        }
        
    }
    
}


