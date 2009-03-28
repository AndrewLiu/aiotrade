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
package org.aiotrade.charting.chart.handledchart;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.charting.chart.Chart;
import org.aiotrade.charting.chart.util.Handle;
import org.aiotrade.charting.chart.util.ValuePoint;
import org.aiotrade.charting.view.pane.DatumPlane;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.charting.view.pane.Pane;
import org.aiotrade.util.swing.action.EditAction;

/**
 *
 * @author Caoyuan Deng
 */

public abstract class AbstractHandledChart<T extends Chart<?>> implements HandledChart<T> {
    
    private final static Cursor HANDLE_CURSOR  = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final static Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final static Cursor MOVE_CURSOR    = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    
    /** @NOTICE should define this to Integer.MAX_VALUE instead of any other values */
    protected final int VARIABLE_NUMBER_OF_HANDLES = Integer.MAX_VALUE;
    
    private DrawingPane drawingPane;
    private boolean paneMouseAdapterForDrawingAdded = false;
    
    private DatumPlane datumPlane;
    private T chart;
    
    /**
     * define final members, so we can be sure that they won't be pointed to
     * another object, even in case of being got by others via public or
     * protected method
     */
    private final List<Handle> currentHandles  = new ArrayList<Handle>();
    private final List<Handle> previousHandles = new ArrayList<Handle>();
    
    /** For moving chart: the valuePoint and handls when mouse is pressed before drag */
    private final List<Handle> currentHandlesWhenMousePressed = new ArrayList<Handle>();
    /**
     * define mousePressedPoint as final to force using copy(..) to set its value
     */
    private final ValuePoint mousePressedPoint = new ValuePoint();
    
    private GeneralPath allCurrentHandlesPathBuf = new GeneralPath();
    
    private int nHandles;
    private int selectedHandleIdx = 0;
    
    private boolean firstStretch = true;
    private boolean accomplished = false;
    private boolean anchored = false;
    private boolean activated = false;
    private boolean readyToDrag = false;
    
    private PaneMouseAdapter paneMouseAdapter;
    
    private Cursor cursor;
    
    private ValuePoint pointBuf = new ValuePoint();
    private List<ValuePoint> handlePointsBuf = new ArrayList<ValuePoint>();
    
    /**
     * @NOTICE
     * Should define a no args constructor explicitly for class.newInstance()
     */
    public AbstractHandledChart() {
        this(null);
    }
    
    public AbstractHandledChart(DrawingPane drawing) {
        this.chart = init();
        this.chart.setDepth(Pane.DEPTH_DRAWING);
        
        if (drawing != null) {
            attachDrawingPane(drawing);
        }
    }
    
    public AbstractHandledChart(DrawingPane drawing, List<ValuePoint> points) {
        init(drawing, points);
    }
    
    protected abstract T init();
    
    /**
     * init with known points
     */
    public void init(DrawingPane drawing, List<ValuePoint> points) {
        assert points != null : "this is for points known HandledChart!";
        
        attachDrawingPane(drawing);
        int size = points.size();
        setNHandles(size);
        
        for (int i = 0; i < size; i++) {
            currentHandles.add(new Hdl());
            previousHandles.add(new Hdl());
            currentHandlesWhenMousePressed.add(new Hdl());
            
            /** assign currentHandles' points to points */
            currentHandles.get(i).copyPoint(points.get(i));
        }
        
        accomplished = true;
        
        /** set chart' arg according to current handles */
        setChartModel(currentHandles);
        
        /** now the chart's arg has been set, and ready to be put to drawing pane */
        drawing.putChart(chart);
        drawing.getView().getMainLayeredPane().moveToBack(drawing);
    }
    
    public void attachDrawingPane(DrawingPane drawing) {
        if (this.drawingPane == null || this.drawingPane != drawing) {
            this.drawingPane = drawing;
            this.datumPlane = drawing.getDatumPlane();
            
            /** should avoid listener being added more than once */
            if (!paneMouseAdapterForDrawingAdded) {
                addMouseAdapterToPane();
            }
            
            assert chart != null : "chart instance should has been created!";
            chart.set(datumPlane, datumPlane.getMasterSer(), Pane.DEPTH_DRAWING);
        }
    }
    
    private void addMouseAdapterToPane() {
        if (drawingPane != null) {
            paneMouseAdapter = new PaneMouseAdapter();
            drawingPane.addMouseListener(paneMouseAdapter);
            drawingPane.addMouseMotionListener(paneMouseAdapter);
            
            paneMouseAdapterForDrawingAdded = true;
        }
    }
    
    protected void setNHandles(int nHandles) {
        this.nHandles = nHandles;
    }
    
    public void removeMouseAdapterOnPane() {
        if (drawingPane != null) {
            drawingPane.removeMouseListener(paneMouseAdapter);
            drawingPane.removeMouseMotionListener(paneMouseAdapter);
            paneMouseAdapter = null;
            
            paneMouseAdapterForDrawingAdded = false;
        }
    }
    
    /**
     *
     *
     * @return <code>true</code> if accomplished after this anchor,
     *         <code>false</code> if not yet.
     */
    private boolean anchorHandle(ValuePoint point) {
        
        if (currentHandles.size() == 0) {
            if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
                /** this is a nHandles variable chart, create first handle */
                currentHandles.add(new Hdl());
                previousHandles.add(new Hdl());
                currentHandlesWhenMousePressed.add(new Hdl());
            } else {
                /** this is a nHandles pre-defined chart, create all of the handles */
                for (int i = 0; i < nHandles; i++) {
                    currentHandles.add(new Hdl());
                    previousHandles.add(new Hdl());
                    currentHandlesWhenMousePressed.add(new Hdl());
                }
            }
        }
        
        currentHandles.get(selectedHandleIdx).copyPoint(point);
        
        if (!accomplished) {
            /** make handles that not yet anchored having the same position as selectedHandle */
            for (int i = selectedHandleIdx + 1, n = currentHandles.size(); i < n; i++) {
                currentHandles.get(i).copyPoint(point);
            }
        }
        
        backupCurrentHandlesToPreviousHandles();
        
        if (selectedHandleIdx < nHandles - 1) {
            anchored = true;
            
            /** select next handle */
            selectedHandleIdx++;
            
            /** create next handle if not created yet */
            if (currentHandles.size() - 1 < selectedHandleIdx) {
                currentHandles.add(new Hdl(point));
                previousHandles.add(new Hdl(point));
                currentHandlesWhenMousePressed.add(new Hdl());
            }
        } else {
            /** if only one handle, should let it be drawn at once */
            if (nHandles == 1) {
                stretchHandle(point);
            }
            
            anchored = false;
            accomplished = true;
            selectedHandleIdx = -1;
        }
        
        return accomplished;
    }
    
    
    private void stretchHandle(ValuePoint point) {
        
        backupCurrentHandlesToPreviousHandles();
        
        /** set selectedHandle's new position */
        currentHandles.get(selectedHandleIdx).copyPoint(point);
        
        if (!accomplished) {
            for (int i = selectedHandleIdx + 1, n = currentHandles.size(); i < n; i++) {
                currentHandles.get(i).copyPoint(currentHandles.get(selectedHandleIdx).getPoint());
            }
        }
        
        
        Graphics g = drawingPane.getGraphics();
        if (g != null) {
            try {
                g.setXORMode(drawingPane.getBackground());
                
                if (firstStretch) {
                    firstStretch = false;
                } else {
                    /** erase previous drawing */
                    renderPrevious(g);
                    previousHandles.get(selectedHandleIdx).render(g);
                }
                /** current new drawing */
                renderCurrent(g);
                currentHandles.get(selectedHandleIdx).render(g);
                
                /** restore to paintMode */
                g.setPaintMode();
                
                if (!accomplished) {
                    for (int i = 0; i < selectedHandleIdx; i++) {
                        currentHandles.get(i).render(g);
                    }
                } else {
                    for (int i = 0, n = currentHandles.size(); i < n; i++) {
                        if (i != selectedHandleIdx) {
                            currentHandles.get(i).render(g);
                        }
                    }
                }
            } finally {
                g.dispose();
            }
        }
    }
    
    private void moveChart(ValuePoint mouseDraggedPoint) {
        
        backupCurrentHandlesToPreviousHandles();
        
        /**
         * should compute bar moved instead of time moved, because when shows
         * in trading date mode, time moved may not be located at a trading day
         */
        int barMoved = datumPlane.bt(mouseDraggedPoint.t) - datumPlane.bt(mousePressedPoint.t);
        float vMoved = mouseDraggedPoint.v - mousePressedPoint.v;
        
        ValuePoint newPoint = new ValuePoint();
        for (int i = 0, n = currentHandles.size(); i < n; i++) {
            ValuePoint oldPoint = currentHandlesWhenMousePressed.get(i).getPoint();
            
            /** compute newTime, process bar fisrt, then transfer to time */
            final int  oldBar  = datumPlane.bt(oldPoint.t);
            final int  newBar  = oldBar + barMoved;
            final long newTime = datumPlane.tb(newBar);
            
            /** compute newValue */
            final float newValue = oldPoint.v + vMoved;
            
            /**
             * @NOTICE
             * do not use getPoint().set(newTime, newValue) to change point member,
             * because we need handle to recompute position. use copyPoint().
             */
            newPoint.set(newTime, newValue);
            currentHandles.get(i).copyPoint(newPoint);
        }
        
        Graphics g = drawingPane.getGraphics();
        if (g != null) {
            try {
                g.setXORMode(drawingPane.getBackground());
                
                /** erase previous drawing */
                renderPrevious(g);
                renderHandles(g, previousHandles);
                
                /** current new drawing */
                renderCurrent(g);
                renderHandles(g, currentHandles);
            } finally {
                g.dispose();
            }
        }
        
    }
    
    private void backupCurrentHandlesToPreviousHandles() {
        for (int i = 0, n = currentHandles.size(); i < n; i++) {
            previousHandles.get(i).copyPoint(currentHandles.get(i).getPoint());
        }
    }
    
    private void renderHandles(Graphics g, List<Handle> handles) {
        for (Handle handle : handles) {
            handle.render(g);
        }
    }
    
    public void activate() {
        this.activated = true;
    }
    
    public void passivate() {
        this.activated = false;
    }
    
    public boolean isActivated() {
        return activated;
    }
    
    public boolean isAccomplished() {
        return accomplished;
    }
    
    private boolean isReadyToDrag() {
        return readyToDrag;
    }
    
    public List<ValuePoint> getCurrentHandlesPoints() {
        return handlesPoints(currentHandles);
    }
    
    protected List<ValuePoint> handlesPoints(List<Handle> handles) {
        handlePointsBuf.clear();
        for (int i = 0, n = handles.size(); i < n; i++) {
            handlePointsBuf.add(handles.get(i).getPoint());
        }
        
        return handlePointsBuf;
    }
    
    private void setReadyToDrag(boolean b) {
        readyToDrag = b;
    }
    
    public T getChart() {
        return chart;
    }
    
    public GeneralPath getAllCurrentHandlesPath() {
        allCurrentHandlesPathBuf.reset();
        for (Handle handle : currentHandles) {
            allCurrentHandlesPathBuf.append(handle.getPath(), false);
        }
        
        return allCurrentHandlesPathBuf;
    }
    
    private Handle getHandleAt(int x, int y) {
        for (Handle handle : currentHandles) {
            if (handle.contains(x, y)) {
                return handle;
            }
        }
        
        return null;
    }
    
    public Cursor getCursor() {
        return cursor;
    }
    
    /**
     * @NOTCIE
     * use this method carefullly:
     * as it always return the same instance, should copy its value instead of
     * set to it or '=' it.
     */
    private ValuePoint p(MouseEvent e) {
        pointBuf.set(datumPlane.tx(e.getX()), datumPlane.vy(e.getY()));
        
        return pointBuf;
    }
    
    private void renderPrevious(Graphics g) {
        setChartModelAndRenderChart(g, previousHandles);
    }
    
    private void renderCurrent(Graphics g) {
        setChartModelAndRenderChart(g, currentHandles);
    }
    
    private void setChartModelAndRenderChart(Graphics g, List<Handle> handles) {
        /** 1. set chart's model according to the handles */
        setChartModel(handles);
        
        /** 2. plot chart, now chart is ready for drawing */
        chart.plot();
        
        /** 3. draw chart using g */
        chart.render(g);
    }
    
    /**
     * set the chart's model according to the handles.
     *
     * @param handles the list of handles to be used to set the model
     */
    protected abstract void setChartModel(List<Handle> handles);
    
    public final int compareTo(HandledChart another) {
        if (this.toString().equalsIgnoreCase(another.toString())) {
            return this.hashCode() < another.hashCode() ? -1 : (this.hashCode() == another.hashCode() ? 0 : 1);
        } else {
            return this.toString().compareTo(another.toString());
        }
    }
    
    public HandledChart createNewInstance() {
        HandledChart handledChart = null;
        try {
            handledChart = this.getClass().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return handledChart;
    }
    
    /**
     * mouse adapter for handling drawing.
     *
     * @NOTICE it will be added to drawingPane instead of HandledChart
     *
     * @NOTICE Since too many process concerning with HandleChart itself, if we define
     * mouse adapter on DrawingPane.java instead of here, we shall open a couple
     * of public method for HandledChart, and transfer the mouse event to here,
     * makeing almost a MouseAdapter for this class. So, we define MouseAdapter
     * here neck and crop.
     *
     * As each HandledChart own a private this listener , which will be added to
     * drawingPane when attachDrawingPane. So, drawingPane may has more than one
     * listener(s). So be careful to define the mouse behaves minding other
     * HandledChart(s).
     */
    public class PaneMouseAdapter extends MouseAdapter implements MouseMotionListener {
        public void mouseClicked(MouseEvent e) {
            /** sinlge-clicked ? go on drawing, or, check my selection status */
            if (e.getClickCount() == 1) {
                
                /** go on drawing ? */
                if (isActivated() && !isAccomplished()) {
                    boolean accomplishedNow = anchorHandle(p(e));
                    if (accomplishedNow) {
                        drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this);
                    }
                    /** always set this is selected in this case: */
                    getChart().setSelected(true);
                }
                /** else, check my selection status */
                else {
                    if (chart.hits(e.getPoint())) {
                        if (getChart().isSelected()) {
                            final EditAction action = getChart().lookupActionAt(EditAction.class, e.getPoint());
                            if (action != null) {
                                /** as the glassPane is always in the front, so add it there */
                                action.anchorEditor(drawingPane.getView().getGlassPane());
                                action.execute();
                            }
                        }
                        
                        getChart().setSelected(true);
                        /**
                         * I was just selected only, don't call activate() here, let drawingPane
                         * to decide if also activate me.
                         */
                    } else {
                        getChart().setSelected(false);
                        /**
                         * I was just deselected only, don't call passivate() here, let drawingPane
                         * to decide if also passivate me.
                         */
                    }
                }
                
            }
            /** double clicked, process chart whose nHandles is variable */
            else {
                
                if (!isAccomplished()) {
                    if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
                        anchored = false;
                        accomplished = true;
                        selectedHandleIdx = -1;
                        
                        drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this);
                    }
                }
            }
        }
        
        public void mousePressed(MouseEvent e) {
            if (isReadyToDrag()) {
                mousePressedPoint.copy(p(e));
                /** record handles when mouse pressed, for moveChart() */
                for (int i = 0, n = currentHandles.size(); i < n; i++) {
                    currentHandlesWhenMousePressed.get(i).copyPoint(currentHandles.get(i).getPoint());
                }
            }
            
            /** @TODO */
            //            if (isAccomplished() && isActive()) {
            //                if (nHandles == VARIABLE_NUMBER_OF_HANDLES) {
            //                    /** edit(add/delete handle) chart whose nHandles is variable */
            //                    if (e.isControlDown()) {
            //                        Handle theHandle = handleAt(e.getX(), e.getY());
            //                        if (theHandle != null) {
            //                            /** delete handle */
            //                            int idx = currentHandles.indexOf(theHandle);
            //                            if (idx > 0) {
            //                                currentHandles.remove(idx);
            //                                previousHandles.remove(idx);
            //                                currentHandlesWhenMousePressed.remove(idx);
            //                            }
            //                        } else {
            //                            /** add handle */
            //                        }
            //                    }
            //                }
            //            }
            
        }
        
        public void mouseReleased(MouseEvent e) {
        }
        
        public void mouseMoved(MouseEvent e) {
            if (isActivated()) {
                if (!isAccomplished()) {
                    if (anchored) {
                        stretchHandle(p(e));
                    }
                }
                /** else, decide what kind of cursor will be used and if it's ready to be moved */
                else {
                    Handle theHandle = getHandleAt(e.getX(), e.getY());
                    /** mouse points to theHandle ? */
                    if (theHandle != null) {
                        int idx = currentHandles.indexOf(theHandle);
                        if (idx >= 0) {
                            selectedHandleIdx = idx;
                        }
                        
                        cursor = HANDLE_CURSOR;
                    }
                    /** else, mouse does not point to any handle */
                    else {
                        selectedHandleIdx = -1;
                        /** mouse points to this chart ? */
                        if (chart.hits(e.getX(), e.getY())) {
                            setReadyToDrag(true);
                            cursor = MOVE_CURSOR;
                        }
                        /** else, mouse does not point to this chart */
                        else {
                            setReadyToDrag(false);
                            cursor = DEFAULT_CURSOR;
                        }
                    }
                }
            }
        }
        
        public void mouseDragged(MouseEvent e) {
            /** only do something when isFinished() */
            if (isActivated() && isAccomplished()) {
                if (selectedHandleIdx != -1) {
                    stretchHandle(p(e));
                } else {
                    if (isReadyToDrag()) {
                        moveChart(p(e));
                    }
                }
                
                /** notice drawingPane */
                drawingPane.accomplishedHandledChartChanged(AbstractHandledChart.this);
            }
        }
    }
    
    /**
     * Inner class of Hdl which implement Handle
     */
    protected class Hdl implements Handle {
        
        private static final int RADIUS = 2;
        
        private ValuePoint point = new ValuePoint();
        
        private GeneralPath bufPath = new GeneralPath();
        private Point2D.Double bufLocation = new Point2D.Double();
        
        public Hdl() {
        }
        
        public Hdl(ValuePoint point) {
            copyPoint(point);
        }
        
        public void copyPoint(ValuePoint src) {
            this.point.copy(src);
        }
        
        public ValuePoint getPoint() {
            return point;
        }
        
        public GeneralPath getPath() {
            /**
             * always replot path as not only point could have been changed,
             * but also the view's size could have been changed
             */
            plot();
            
            return bufPath;
        }
        
        private void plot() {
            Point2D location = getLocation();
            
            final float x = (float)location.getX();
            final float y = (float)location.getY();
            
            bufPath.reset();
            bufPath.moveTo(x - RADIUS, y - RADIUS);
            bufPath.lineTo(x - RADIUS, y + RADIUS);
            bufPath.lineTo(x + RADIUS, y + RADIUS);
            bufPath.lineTo(x + RADIUS, y - RADIUS);
            bufPath.closePath();
        }
        
        private Point2D getLocation() {
            final double x = datumPlane.xb(datumPlane.bt(point.t));
            final double y = datumPlane.yv(point.v);
            
            bufLocation.setLocation(x, y);
            
            return bufLocation;
        }
        
        public boolean contains(int x, int y) {
            /**
             * always recompute location as not only point could have been changed,
             * but also the view's size could have been changed
             */
            Point2D location = getLocation();
            
            final double centerx = location.getX();
            final double centery = location.getY();
            
            if (x <= centerx + RADIUS && x >= centerx - RADIUS && y <= centery + RADIUS && y >= centery - RADIUS) {
                return true;
            } else {
                return false;
            }
        }
        
        public void render(Graphics g) {
            g.setColor(LookFeel.getCurrent().handleColor);
            ((Graphics2D)g).draw(getPath());
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Handle) {
                Handle another = (Handle)o;
                if (this.point.equals(another.getPoint())) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
}



