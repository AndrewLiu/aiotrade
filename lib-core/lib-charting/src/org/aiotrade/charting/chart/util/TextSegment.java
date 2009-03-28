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
package org.aiotrade.charting.chart.util;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 * @author Caoyuan Deng
 */
public class TextSegment extends AbstractSegment {
    private String text;
    private float x;
    private float y;
    private Color bgColor;
    
    private boolean valid;
    private Rectangle bounds = new Rectangle();
    
    public TextSegment() {
    }
    
    public TextSegment(String text, float x, float y, Color color, Color bgColor) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.bgColor = bgColor;
        this.valid = false;
    }
    
    public TextSegment(String text, float x, float y, Color color) {
        this(text, x, y, color, null);
    }
    
    public void setText(String text) {
        this.text = text;
        this.valid = false;
    }
    
    private void computeBounds(Graphics g) {
        final FontMetrics fm = g.getFontMetrics();
        bounds.setBounds(
                Math.round(x), Math.round(y) - fm.getHeight() + 1,
                fm.stringWidth(text) + 1, fm.getHeight());
    }
    
    public Rectangle getBounds(Graphics g) {
        if (! valid) {
            computeBounds(g);
        }
        
        return bounds;
    }
    
    public void render(Graphics g) {
        if (bgColor != null) {
            Rectangle bounds = getBounds(g);
            g.setColor(bgColor);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        
        g.setColor(color);
        ((Graphics2D)g).drawString(text, x, y);
    }
}