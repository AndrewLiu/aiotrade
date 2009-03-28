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

import java.awt.geom.GeneralPath;
import org.aiotrade.lib.charting.widget.WidgetModel;
import org.aiotrade.lib.charting.widget.StickBar.Model;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 29, 2006, 5:27 PM
 * @since   1.0.4
 */
public class StickBar extends PathWidget<Model> {
    public final static class Model implements WidgetModel {
        float xCenter;
        float y1;
        float y2;
        float width;
        boolean thin;
        boolean filled;
        
        public void set(float xCenter, float y1, float y2, float width, boolean thin, boolean filled) {
            this.xCenter = xCenter;
            this.y1 = y1;
            this.y2 = y2;
            this.width = width;
            this.thin = thin;
            this.filled = filled;
        }
    }
    
    
    public StickBar() {
        super();
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    protected void plotWidget() {
        final Model model = model();
        final GeneralPath path = getPath();
        path.reset();
        
        final float xRadius = model.width < 2 ? 0 : (model.width - 2) / 2;
        
        if (model.thin || model.width <= 2) {
            path.moveTo(model.xCenter, model.y1);
            path.lineTo(model.xCenter, model.y2);
        } else {
            path.moveTo(model.xCenter - xRadius, model.y1);
            path.lineTo(model.xCenter - xRadius, model.y2);
            path.lineTo(model.xCenter + xRadius, model.y2);
            path.lineTo(model.xCenter + xRadius, model.y1);
            path.closePath();
            
            if (model.filled) {
                for (int i = 1; i < model.width - 2; i++) {
                    path.moveTo(model.xCenter - xRadius + i, model.y1);
                    path.lineTo(model.xCenter - xRadius + i, model.y2);
                }
            }
            
        }
    }
    
}
