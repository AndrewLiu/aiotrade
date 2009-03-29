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
package org.aiotrade.lib.charting.chart.handledchart;

import java.awt.Color;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.charting.chart.AbstractChart;
import org.aiotrade.lib.charting.chart.handledchart.TextChart.Model;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.widget.Label;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.w3c.dom.Element;

/**
 *
 * @author Caoyuan Deng
 */
public class TextChart extends AbstractChart<Model> {
    public final static class Model implements WidgetModel {
        long  t1;
        float v1;
        String text;
        
        public Model() {}
        
        public Model(long t1, float v1, String text) {
            set(t1, v1, text);
        }
        
        public void set(long t1, float v1, String text) {
            this.t1 = t1;
            this.v1 = v1;
            this.text = text;
        }

        public void set(long t1, float v1) {
            this.t1 = t1;
            this.v1 = v1;
        }
        
        public Element writeToBean(BeansDocument doc) {
            final Element bean = doc.createBean(this);
            
            doc.valueConstructorArgOfBean(bean, 0, t1);
            doc.valueConstructorArgOfBean(bean, 1, v1);
            doc.valueConstructorArgOfBean(bean, 2, text);
            
            return bean;
        }
    }
    
    /** this label is part of TextChart, don't try to release it */
    final private Label label = new Label();
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotChart() {
        final Model model = model();
        
        Color color = LookFeel.getCurrent().drawingColor;
        setForeground(color);
        
        final float x = xb(bt(model.t1));
        final float y = yv(model.v1);
        
        addChild(label);
        label.setFont(LookFeel.getCurrent().axisFont);
        label.setForeground(color);
        label.model().set(x, y, model.text);
        label.plot();
    }
}




