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
package org.aiotrade.lib.charting.util;

import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 9:36 PM
 * @since   1.0.4
 */
public class GeomUtil {
    
    public final static float hOne(double vRange, double hRange) {
        return (float)(vRange == 0 ? 1 : hRange / vRange);
    }
    
    public final static int yv(double v, double hOne, double vMin, double yLower) {
        return (int)-((hOne * (v - vMin) - yLower));
    }
    
    public final static float vy(double y, double hOne, double vMin, double yLower) {
        return  (float)-((y - yLower) / hOne - vMin);
    }
    
    public final static float yOfLine(double x, double baseX, double baseY, double k) {
        return (float)(baseY + (x - baseX) * k);
    }
    
    /**
     * @param x
     * @param xCenter center point x of arc
     * @param yCenter center point y of arc
     * @return y or NaN
     */
    public final static float yOfCircle(double x, double xCenter, double yCenter, double radius, boolean positiveSide) {
        final double dx = x - xCenter;
        final double dy = Math.sqrt(radius * radius - dx * dx);
        return (float)(positiveSide ? yCenter + dy : yCenter - dy);
    }
    
    public final static float yOfCircle(double x, Arc2D circle, boolean positiveSide) {
        final double xCenter = circle.getCenterX();
        final double yCenter = circle.getCenterY();
        final double radius = circle.getHeight() / 2.0;
        return yOfCircle(x, xCenter, yCenter, radius, positiveSide);
    }
    
    public final static float distanceToCircle(double x, double y, Arc2D circle) {
        final double xCenter = circle.getCenterX();
        final double yCenter = circle.getCenterY();
        final double radius  = circle.getHeight() / 2.0;
        final double dx = x - xCenter;
        final double dy = y - yCenter;
        return (float)(Math.sqrt(dx * dx + dy * dy) - radius);
    }
    
    public final static boolean samePoint(double x1, double y1, double x2, double y2) {
        return (int)x1 == (int)x2 && (int)y1 == (int)y2;
    }
    
}
