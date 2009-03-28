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
package org.aiotrade.charting.widget;

import java.awt.geom.GeneralPath;
import org.aiotrade.charting.widget.DiamondDot.Model;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 4:11 PM
 * @since   1.0.4
 */
public class DiamondDot extends PathWidget<Model> {
    public final static class Model implements WidgetModel {
        float x;
        float y;
        float width;
        boolean filled;
        
        public void set(float x, float y, float width, boolean filled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.filled = filled;
        }
    }
    
    public DiamondDot() {
        super();
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotWidget() {
        final Model model = model();
        final GeneralPath path = getPath();
        path.reset();
        
        final int radius = model.width > 2 ? 2 : 1;
        if (model.width <= 2) {
            path.moveTo(model.x, model.y);
            path.lineTo(model.x, model.y);
        } else {
            path.moveTo(model.x - radius, model.y);
            path.lineTo(model.x, model.y - radius);
            path.lineTo(model.x + radius, model.y);
            path.lineTo(model.x, model.y + radius);
            path.lineTo(model.x - radius, model.y);
        }
    }
    
}
