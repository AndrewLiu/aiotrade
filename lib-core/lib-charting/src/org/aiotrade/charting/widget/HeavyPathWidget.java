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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:38 AM
 * @since   1.0.4
 */
public class HeavyPathWidget<M extends WidgetModel> extends AbstractWidget<M> {
    
    private final Map<Color, GeneralPath> colorsWithPath = new HashMap<Color, GeneralPath>();
    
    public HeavyPathWidget() {
        super();
    }
    
    protected M createModel() {
        return null;
    }
    
    @Override
    public boolean isContainerOnly() {
        return false;
    }
    
    @Override
    protected Rectangle makePreferredBounds() {
        Rectangle pathBounds = new Rectangle();
        for (GeneralPath path : colorsWithPath.values()) {
            pathBounds.add(path.getBounds());
        }
        
        return new Rectangle(
                pathBounds.x, pathBounds.y,
                pathBounds.width + 1, pathBounds.height + 1);
    }
    
    public GeneralPath getPath(Color color) {
        GeneralPath path = colorsWithPath.get(color);
        if (path == null) {
            path = borrowPath();
            colorsWithPath.put(color, path);
        }
        return path;
    }
    
    public void appendFrom(PathWidget<?> pathWidget) {
        final Color color = pathWidget.getForeground();
        getPath(color).append(pathWidget.getPath(), false);
    }
    
    @Override
    protected boolean widgetContains(double x, double y, double width, double height) {
        for (GeneralPath path : colorsWithPath.values()) {
            if (path.contains(x, y, width, height)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean widgetIntersects(double x, double y, double width, double height) {
        for (GeneralPath path : colorsWithPath.values()) {
            if (path.intersects(x, y, width, height)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void renderWidget(Graphics g0) {
        final Graphics2D g = (Graphics2D)g0;
        
        for (Color color : colorsWithPath.keySet()) {
            g.setColor(color);
            g.draw(colorsWithPath.get(color));
        }
    }
    
    @Override
    public void reset() {
        super.reset();
        for (GeneralPath path : colorsWithPath.values()) {
            path.reset();
        }
    }
    
    @Override
    protected void plotWidget() {
    }
    
    @Override
    protected void finalize() throws Throwable {
        for (GeneralPath path : colorsWithPath.values()) {
            returnPath(path);
        }
        
        super.finalize();
    }
    
}

