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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.charting.chart.Chart.StrockType;
import org.aiotrade.charting.util.GeomUtil;
import org.aiotrade.charting.widget.WidgetModel;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.charting.view.pane.DatumPlane;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.widget.AbstractWidget;
import org.aiotrade.util.serialization.BeansDocument;
import org.aiotrade.util.ReferenceOnly;
import org.w3c.dom.Element;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractChart<M extends WidgetModel> extends
        AbstractWidget<M> implements
        Chart<M> {
    
    protected final static int MARK_INTERVAL = 16;
    
    private final static Color COLOR_SELECTED = new Color(0x447BCD);
    private final static Color COLOR_HIGHLIGHTED = COLOR_SELECTED.darker();
    private final static Color COLOR_HOVERED = COLOR_SELECTED.brighter();
    
    private final static Stroke[] BASE_STROKES = new Stroke[] {
        new BasicStroke(1.0f),
        new BasicStroke(2.0f)
    };
    private final static float[] DASH_PATTERN = {5, 2};
    private final static Stroke[] DASH_STROKES = new Stroke[] {
        new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0),
        new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0)
    };
    
    private List<Point> markPoints = new ArrayList<Point>(); // used to draw selected mark
    
    /** Component that charts x-y based on */
    protected DatumPlane datumPlane;
    
    /** masterSer that will be got from: datumPlane.getMasterSer() */
    protected MasterSer masterSer;
    
    protected Ser ser;
    
    /**
     * the depth of this chart in pane,
     * the chart's container will decide the chart's defaultColor according to the depth
     */
    private int depth;
    
    private float strockWidth = 1.0f;
    private StrockType strockType = StrockType.Base;
    
    /**
     * Keep convenient references to datumPane's geometry, thus we can also
     * shield the changes from datumPane.
     */
    protected   int nBars;
    protected float wBar;
    
    /**
     * To allow the mouse pick up accurately a chart, we need seperate a chart to
     * a lot of segment, each segment is a shape that could be sensible for the
     * mouse row. The minimum segment's width is defined here.
     *
     * Although we can define it > 1, such as 3 or 5, but, when 2 bars or more
     * are located in the same one segment, they can have only one color,
     * example: two-colors candle chart. So, we just simplely define it as 1.
     *
     * Another solution is define 1 n-colors chart as n 1-color charts (implemented).
     */
    private final static int MIN_SEGMENT_WIDTH = 1;
    protected float wSeg = MIN_SEGMENT_WIDTH;
    protected int nSegs;
    
    protected int nBarsCompressed = 1;
    
    private boolean selected;
    private boolean firstPlotting;
    
    public AbstractChart() {
        super();
    }
    
    /** @TODO */
    @Override
    public boolean isContainerOnly() {
        return true;
    }
    
    /**
     * NOTICE
     * It's always better to set datumPlane here.
     * After call following set(,,,) methods, the chart can be put in the any
     * pane that has this datumPlane referenced by pane.putChart() for
     * automatical drawing, or, can be drawn on these pane by call pane.render(g)
     * initiatively (such as mouse cursor chart).
     * So, do not try to separate a setPane(Pane) method.
     */
    public void set(DatumPlane datumPane, Ser ser, int depth) {
        this.datumPlane  = datumPane;
        this.ser = ser;
        this.depth = depth;
    }
    
    public void set(DatumPlane datumPane, Ser ser) {
        set(datumPane, ser, this.depth);
    }
    
    public void setFirstPlotting(boolean b) {
        this.firstPlotting = b;
    }
    
    public boolean isFirstPlotting() {
        return firstPlotting;
    }
    
    /**
     * present only prepare the chart's pathSegs and textSegs, but not really render,
     * should call render(Graphics2D g) to render this chart upon g
     */
    protected void plotWidget() {
        this.masterSer = datumPlane.getMasterSer();
        this.nBars     = datumPlane.getNBars();
        this.wBar      = datumPlane.getWBar();
        
        this.wSeg = Math.max(wBar, MIN_SEGMENT_WIDTH);
        this.nSegs = (int)(nBars * wBar / wSeg) + 1;
        
        this.nBarsCompressed = wBar >= 1 ? 1 : (int)(1 / wBar);
        
        reset();
        
        plotChart();
    }
    
    protected abstract void plotChart();
    
    @Override
    public void reset() {
        super.reset();

        markPoints.clear();
    }
    
    protected void addMarkPoint(int x, int y) {
        markPoints.add(new Point(x, y));
    }
    
    /**
     * use intersects instead of contains here, contains means:
     * A specified coordinate is inside the boundary of this chart.
     * But not each kind of chart has boundary.
     * For example: in the case of Line, objects it always contains nothing,
     * since a line contains no area.
     */
    @Override
    protected boolean widgetIntersects(double x, double y, double width, double height) {
        return false;
    }
    
    protected void renderWidget(Graphics g0) {
        final Graphics2D g = (Graphics2D)g0;
        
        int w = (int)getStrockWidth();
        Stroke stroke = null;
        switch (getStrockType()) {
            case Base:
                stroke = w <= BASE_STROKES.length ?
                    BASE_STROKES[w - 1] :
                    new BasicStroke(w);
                break;
            case Dash:
                stroke = w <= DASH_STROKES.length ?
                    stroke = DASH_STROKES[w - 1] :
                    new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0);
                break;
            default:
                stroke = new BasicStroke(w);
                break;
        }
        g.setStroke(stroke);
        
        if (isSelected()) {
            for (Point point : markPoints) {
                renderMarkAtPoint(g, point);
            }
        }
    }
    
    private void renderMarkAtPoint(Graphics g, Point point) {
        g.setColor(LookFeel.getCurrent().handleColor);
        g.fillRect(point.x - 2, point.y - 2, 5, 5);
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean b) {
        this.selected = b;
    }
    
    public void setStrock(int width, StrockType type) {
        this.strockWidth = width;
        this.strockType = type;
    }
    
    /**
     * @return width of chart. not width of canvas!
     */
    public float getStrockWidth() {
        return strockWidth;
    }
    
    public StrockType getStrockType() {
        return strockType;
    }
    
    public void setSer(Ser ser) {
        this.ser = ser;
    }
    
    public Ser getSer() {
        return ser;
    }
    
    /**
     * Translate barIndex to X point for drawing
     *
     * @param barIndex: index of bars, start from 1 to nBars
     */
    protected final float xb(int barIndex) {
        return this.datumPlane.xb(barIndex);
    }
    
    /**
     * Translate value to Y point for drawing
     * @param value
     */
    protected final float yv(float value) {
        return this.datumPlane.yv(value);
    }
    
    protected final int bx(float x) {
        return this.datumPlane.bx(x);
    }
    
    protected final float vy(float y) {
        return this.datumPlane.vy(y);
    }
    
    /**
     * @return row in ser corresponding to barIndex
     */
    protected final int rb(int barIndex) {
        return this.datumPlane.rb(barIndex);
    }
    
    protected final int br(int row) {
        return this.datumPlane.br(row);
    }
    
    /**
     * @return segment index corresponding to barIdx
     */
    protected final int sb(int barIdx) {
        return (int)(barIdx * wBar / wSeg) + 1;
    }
    
    protected final int bs(int segIdx) {
        return (int)(((segIdx - 1) * wSeg) / wBar);
    }
    
    /**
     * @param barIdx: index of bars, start from 1 to nBars
     */
    protected final long tb(int barIdx) {
        return this.datumPlane.tb(barIdx);
    }
    
    protected final int bt(long time) {
        return this.datumPlane.bt(time);
    }
    
    protected void plotLine(float xBase, float yBase, float k, final GeneralPath path) {
        final float xBeg = 0;
        final float yBeg = GeomUtil.yOfLine(xBeg, xBase, yBase, k);
        final float xEnd = datumPlane.getWidth();
        final float yEnd = GeomUtil.yOfLine(xEnd, xBase, yBase, k);
        path.moveTo(xBeg, yBeg);
        path.lineTo(xEnd, yEnd);
    }

    protected void plotVerticalLine(int bar, final GeneralPath path) {
        final float x = xb(bar);
        final float yBeg = datumPlane.getYCanvasLower();
        final float yEnd = datumPlane.getYCanvasUpper();
        path.moveTo(x, yBeg);
        path.lineTo(x, yEnd);
    }

    protected void plotLineSegment(float xBeg, float yBeg, float xEnd, float yEnd, final GeneralPath path) {
        path.moveTo(xBeg, yBeg);
        path.lineTo(xEnd, yEnd);
    }

    protected void plotVerticalLineSegment(int bar, float yBeg, float yEnd, final GeneralPath path) {
        final float x = xb(bar);
        path.moveTo(x, yBeg);
        path.lineTo(x, yEnd);
    }

    /** compare according to the depth of chart, used for SortedSet<Chart> */
    public final int compareTo(Chart<?> another) {
        if (this.getDepth() == another.getDepth()) {
            return this.hashCode() < another.hashCode() ? -1 : (this.hashCode() == another.hashCode() ? 0 : 1);
        } else {
            return this.getDepth() < another.getDepth() ? -1 : 1;
        }
    }
    
    public Element writeToBean(BeansDocument doc) {
        final Element bean = doc.createBean(this);
        
        final Element foregroundBean = doc.createBean(getForeground());
        doc.innerPropertyOfBean(bean, "foreground", foregroundBean);
        doc.valueConstructorArgOfBean(foregroundBean, 0, getForeground().getRed());
        doc.valueConstructorArgOfBean(foregroundBean, 1, getForeground().getGreen());
        doc.valueConstructorArgOfBean(foregroundBean, 2, getForeground().getBlue());
        
        return bean;
    }
    
    /**
     * @ReferenceOnly methods:
     * ----------------------------------------------------------------
     */
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotLine_seg(float xCenter, float yCenter, float k, GeneralPath path) {
        float xlast = xb(0); // bar 0
        float ylast = Float.NaN;
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            float x1 = xlast;
            float y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k);
            
            float x2 = xb(bar);
            float y2 = GeomUtil.yOfLine(x2, xCenter, yCenter, k);
            
            /**
             * if (xlast, y1) is the same point of (xlast, ylast), let
             *     x1 = xlast + 1
             * to avoid the 1 point intersect at the each path's
             * end point, especially in XOR mode:
             */
            while (x1 < x2) {
                if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
                    x1++;
                    y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k);
                } else {
                    break;
                }
            }
            
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            
            ylast = y2;
            
            xlast = x2;
        }
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotLineSegment_seg(float xBeg, float yBeg, float xEnd, float yEnd, GeneralPath path) {
        float dx = xEnd - xBeg;
        float dy = yEnd - yBeg;
        
        float k = (dx == 0) ? 1 : dy / dx;
        float xmin = Math.min(xBeg, xEnd);
        float xmax = Math.max(xBeg, xEnd);
        float ymin = Math.min(yBeg, yEnd);
        float ymax = Math.max(yBeg, yEnd);
        
        float xlast = xb(0); // bar 0
        float ylast = Float.NaN;
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            float x1 = xlast;
            float x2 = xb(bar);
            
            float y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k);
            float y2 = GeomUtil.yOfLine(x2, xBeg, yBeg, k);
            
            
            if (x1 >= xmin && x1 <= xmax && x2 >= xmin && x2 <= xmax &&
                    y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax
                    ) {
                
                /**
                 * if (xlast, y1) is the same point of (xlast, ylast), let
                 *     x1 = xlast + 1
                 * to avoid the 1 point intersect at the each path's
                 * end point, especially in XOR mode:
                 */
                while (x1 < x2) {
                    if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
                        x1++;
                        y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k);
                    } else {
                        break;
                    }
                }
                
                path.moveTo(x1, y1);
                path.lineTo(x2, y2);
                
                ylast = y2;
                
            }
            
            xlast = x2;
            
        }
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotVerticalLine_seg(int bar, GeneralPath path) {
        if (bar >= 1 && bar <= nBars) {
            
            float y1 = yv(datumPlane.getMinValue());
            float y2 = yv(datumPlane.getMinValue());
            float x = xb(bar);
            
            path.moveTo(x, y1);
            path.lineTo(x, y2);
            
        }
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotVerticalLineSegment_seg(int bar, float yBeg, float yEnd, GeneralPath path) {
        if (bar >= 1 && bar <= nBars) {
            
            float x = xb(bar);
            
            path.moveTo(x, yBeg);
            path.lineTo(x, yEnd);
            
        }
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotArc_seg(float xCenter, float yCenter, double radius, GeneralPath path) {
        plotHalfArc_seg(xCenter, yCenter, radius, true, path);
        plotHalfArc_seg(xCenter, yCenter, radius, false, path);
    }
    
    /**
     * @deprecated
     */
    @ReferenceOnly private void plotHalfArc_seg(float xCenter, float yCenter, double radius, boolean positiveSide, GeneralPath path) {
        float xlast = xb(0); // bar 0
        float ylast = Float.NaN;
        for (int bar = 1; bar <= nBars; bar += nBarsCompressed) {
            
            float x1 = xlast;
            float x2 = xb(bar);
            
            /** draw positive arc from x1 to x2 */
            float y1 = GeomUtil.yOfCircle(x1, xCenter, yCenter, radius, positiveSide);
            
            /**
             * if (xlast, y1) is the same point of (xlast, ylast), let
             *     x1 = xlast + 1
             * to avoid the 1 point intersect at the each path's
             * end point, especially in XOR mode:
             *
             * In case of: step = (xfactor <= 2) ? 3 : 1, following code could be ignored:
             *
             * if (isTheSamePoint(x1, y1, xlast, ylast)) {
             *     x1 = xlast + 1;
             *     y1 = yOfArc(x1, xCenter, yCenter, radius, positiveSide);
             * }
             *
             */
            
            
            if (!Float.isNaN(y1)) {
                path.moveTo(x1, y1);
                
                for (float x = x1 + 1; x <= x2; x++) {
                    float y =  GeomUtil.yOfCircle(x, xCenter, yCenter, radius, positiveSide);
                    
                    if (!Float.isNaN(y)) {
                        path.lineTo(x, y);
                        
                        ylast = y;
                    }
                    
                }
            }
            
            xlast = x2;
        }
    }
    
    
}