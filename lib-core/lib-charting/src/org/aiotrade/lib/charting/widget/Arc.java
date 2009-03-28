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
package org.aiotrade.lib.charting.widget;

import java.awt.geom.Arc2D;
import org.aiotrade.lib.charting.widget.Arc.Model;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.w3c.dom.Element;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, Dec 3, 2006, 7:38 AM
 * @since   1.0.4
 */
public class Arc extends ShapeWidget<Arc2D, Model> {
    public final static class Model implements WidgetModel {
        float x;
        float y;
        float width;
        float height;
        float start;
        float extent;
        int closure;
        
        public Model() {}
        
        public Model(float x, float y, float w, float h, float angSt, float angExt, int closure) {
            set(x, y, w, h, angSt, angExt, closure);
        }
        
        public void set(float x, float y, float w, float h, float angSt, float angExt, int closure) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = angSt;
            this.extent = angExt;
            this.closure = closure;
        }
        
        public Element writeToBean(BeansDocument doc) {
            final Element bean = doc.createBean(this);
            
            doc.valueConstructorArgOfBean(bean, 0, x);
            doc.valueConstructorArgOfBean(bean, 1, y);
            doc.valueConstructorArgOfBean(bean, 2, width);
            doc.valueConstructorArgOfBean(bean, 3, height);
            doc.valueConstructorArgOfBean(bean, 4, start);
            doc.valueConstructorArgOfBean(bean, 5, closure);
            
            return bean;
        }
    }
    
    public Arc() {
        super();
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected Arc2D createShape() {
        return new Arc2D.Float();
    }
    
    @Override
    protected boolean widgetIntersects(double x, double y, double width, double height) {
        final Arc2D arc = getShape();
        if (arc.intersects(x, y, width, height)) {
            final int backupClosure = arc.getArcType();
            arc.setArcType(Arc2D.CHORD);
            boolean result = arc.contains(x, y, width, height) ? false : true;
            arc.setArcType(backupClosure);
            
            return result;
        }
        return false;
    }
    
    protected void plotWidget() {
        final Model model = model();
        getShape().setArc(
                model.x, model.y, model.width, model.height,
                model.start, model.extent, model.closure);
    }
    
}


