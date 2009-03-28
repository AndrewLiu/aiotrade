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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.Calendar;
import org.aiotrade.charting.widget.HeavyPathWidget;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.math.StatisticFunction;
import org.aiotrade.math.timeseries.SerItem;
import org.aiotrade.math.timeseries.Var;
import org.aiotrade.charting.chart.ProfileChart.Model;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.laf.LookFeel;

/**
 *
 * @author Caoyuan Deng
 */
public class ProfileChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        Var var;
        
        public void set(Var var) {
            this.var = var;
        }
    }

    private HeavyPathWidget heavyPathWidget = new HeavyPathWidget();
    private Calendar cal = Calendar.getInstance();
    private long time;
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
    }
    
    public void render(Graphics g) {
        final Model model = model();
        
        final int w = datumPlane.getWidth();
        final int h = datumPlane.getHeight();
        
        Color color = LookFeel.getCurrent().getGradientColor(getDepth(), -10);
        setForeground(color);
        
        int width = (int)(w * 2.386);
        
        //color = Color.YELLOW;//new Color(1.0f, 1.0f, 1.0f, 0.618f);
        
        ChartingController controller = datumPlane.getView().getController();
        
        time = controller.getReferCursorTime();
        
        float xorigin = xb(bt(time));
        
        final GeneralPath path = heavyPathWidget.getPath(color);
        
        SerItem item = ser.getItem(time);
        
        Float[][] mass = null;
        if (item != null) {
            Object value = item.get(model.var);
            if (value instanceof Float[][]) {
                mass = (Float[][])value;
            }
        }
        
        if (mass != null) {
            plotProfileChart(mass, xorigin, width, path);
            g.setColor(color);
            ((Graphics2D)g).fill(path);
            //g.draw(path);
        }
        
        
        path.reset();
    }
    
    
    private void plotProfileChart(Float[][] profile, float xorigin, float width, GeneralPath path) {
        int nIntervals = profile[StatisticFunction.VALUE].length - 1;
        
        float halfInterval = (nIntervals < 1) ?
            0:
            0.5f * (profile[StatisticFunction.VALUE][1] - profile[StatisticFunction.VALUE][0]);
        
        boolean firstValueGot = false;
        
        float y = Float.NaN;
        for (int i = 0; i <= nIntervals; i++) {
            
            float mass = profile[StatisticFunction.MASS][i];
            if (!Float.isNaN(mass)) {
                float x = xorigin + mass * width;
                y = yv(profile[StatisticFunction.VALUE][i] + halfInterval);
                if (!firstValueGot) {
                    path.moveTo(xorigin, y);
                    firstValueGot = true;
                } else {
                    path.lineTo(x, y);
                }
                
            }
        }
        
        if (firstValueGot) {
            path.lineTo(xorigin, y);
            path.closePath();
        }
    }
    
}



