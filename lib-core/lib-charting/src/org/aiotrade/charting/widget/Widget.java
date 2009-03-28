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
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.Action;

/**
 * bounds(x, y, width, height) is relative to location(lx, ly), so the final
 * position of widget will be shifted by offset: location(lx, ly upon to
 * (bounds.x, bounds.y). That is, the coordinate (x, y) of bounds' left-top
 * corner relative to origin point(0, 0) will be (x + lx, y + ly).
 *
 * We can use the different bounds + offset(location) combination to define the
 * postion of widget and move it.
 *
 * origin(0,0)
 * +------------------------------------------------------> x
 * |
 * |    * location(lx, ly)
 * |     \
 * |      \
 * |       \ (x + lx, y + ly)
 * |        +------------+  -
 * |        |            |  |
 * |        |   bounds   | height
 * |        |            |  |
 * |        +------------+  _
 * |        |--  width --|
 * |
 * |
 * V
 * y
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:21 AM
 * @since   1.0.4
 */
public interface Widget<M extends WidgetModel> {
    
    void setOpaque(boolean opaque);
    boolean isOpaque();
    
    void setBackground(Paint paint);
    Paint getBackground();
    
    void setForeground(Color color);
    Color getForeground();
    
    void setLocation(Point location);
    void setLocation(double x, double y);
    Point getLocation();
    
    void setBounds(Rectangle rect);
    void setBounds(double x, double y, double width, double height);
    Rectangle getBounds();
    
    boolean contains(Point point);
    boolean contains(double x, double y);
    boolean contains(Rectangle rect);
    boolean contains(double x, double y, double width, double height);
    boolean intersects(Rectangle rect);
    boolean intersects(double x, double y, double width, double height);
    boolean hits(Point point);
    boolean hits(double x, double y);
    
    M model();
    
    void plot();
    void render(Graphics g);
    void reset();

    boolean isContainerOnly();
    
    <T extends Widget<?>> T addChild(T child);
    void removeChild(Widget<?> child);
    List<Widget<?>> getChildren();
    void clearChildren();
    <T extends Widget<?>> List<T> lookupChildren(Class<T> widgetType, Color foreground);
    <T extends Widget<?>> T lookupFirstChild(Class<T> widgetType, Color foreground);
    
    Action addAction(Action action);
    <T extends Action> T lookupAction(Class<T> type);
    <T extends Action> T lookupActionAt(Class<T> type, Point point);
    

    
}
