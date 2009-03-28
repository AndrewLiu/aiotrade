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
package org.aiotrade.charting.view.pane;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import org.aiotrade.charting.chart.Chart;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartValidityObserver;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.util.awt.AWTUtil;

/**
 *
 * @author Caoyuan Deng
 */
public class ChartPane extends AbstractDatumPlane {
    
    private LookFeel colorTheme;
    
    private boolean mouseEntered;
    
    private int yMouse;
    
    private boolean chartValid;
    
    public ChartPane(ChartView view) {
        super(view);
        
        setOpaque(false);
        setRenderStrategy(RenderStrategy.NoneBuffer);
        
        MyMouseAdapter mouseAdapter = new MyMouseAdapter();
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        
        ComponentListener componentListener = new ComponentListener() {
            public void componentHidden(ComponentEvent e) {
                for (Chart chart : getCharts()) {
                    chart.reset();
                }
                chartValid = false;
            }
            
            public void componentMoved(ComponentEvent e) {
            }
            
            public void componentResized(ComponentEvent e) {
                chartValid = false;
            }
            
            public void componentShown(ComponentEvent e) {
            }
        };
        addComponentListener(componentListener);
        
        getView().getController().addObserver(this, new ChartValidityObserver<ChartingController>() {
            public void update(ChartingController controller) {
                chartValid = false;
                setGeometryValid(false);
            }
        });
        
        getView().addObserver(this, new ChartValidityObserver<ChartView>() {
            public void update(ChartView subject) {
                chartValid = false;
                setGeometryValid(false);
            }
        });
    }
    
    protected boolean isChartValid() {
        return chartValid && isGeometryValid();
    }
    
    @Override
    protected void plotPane() {
        colorTheme = LookFeel.getCurrent();
    }
    
    public int getYMouse() {
        return yMouse;
    }
    
    public void setYMouse(int ymouse) {
        this.yMouse = ymouse;
    }
    
    public boolean isMouseEntered() {
        return mouseEntered;
    }
    
    @Override
    protected void finalize() throws Throwable {
        view.getController().removeObserversOf(this);
        view.removeObserversOf(this);
        
        AWTUtil.removeAllAWTListenersOf(this);

        super.finalize();
    }
    
    private class MyMouseAdapter extends MouseAdapter implements MouseMotionListener {
        
        int oldBMouse = -Integer.MAX_VALUE;
        int oldYMouse = -Integer.MAX_VALUE;
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger()) {
                return;
            }
            
            if (!view.isInteractive()) {
                /**
                 * we don't want the click changes the refer cursor position and
                 * selects a chart etc.
                 */
                return;
            }
            
            if (view instanceof WithDrawingPane) {
                DrawingPane drawing = ((WithDrawingPane)view).getSelectedDrawing();
                if (drawing != null && drawing.isInDrawing()) {
                    return;
                }
            }
            
            if (e.isControlDown()) {
                if (!(view.getParent() instanceof ChartViewContainer)) {
                    return;
                }
                ChartViewContainer viewContainer = (ChartViewContainer)view.getParent();
                Chart selectedChart = viewContainer.getSelectedChart();
                Chart theChart = getChartAt(e.getX(), e.getY());
                if (theChart != null) {
                    if (theChart == selectedChart) {
                        /** deselect it */
                        viewContainer.setSelectedChart(null);
                    } else {
                        viewContainer.setSelectedChart(theChart);
                    }
                } else {
                    viewContainer.setSelectedChart(null);
                }
            } else {
                /** set refer cursor */
                int y = e.getY();
                int b = bx(e.getX());
                if (y >= ChartView.TITLE_HEIGHT_PER_LINE && y <= (getHeight() - ChartView.CONTROL_HEIGHT) &&
                        b >= 1 && b <= getNBars()) {
                    int position = rb(b);
                    view.getController().setReferCursorByRow(position, true);
                }
            }
            
        }
        
        public void mouseMoved(MouseEvent e) {
            final int y = e.getY();
            
            if (y >= ChartView.TITLE_HEIGHT_PER_LINE && y <= getHeight() - ChartView.CONTROL_HEIGHT) {
                mouseEntered = true;
                view.getController().setMouseEnteredAnyChartPane(true);
            } else {
                mouseEntered = false;
                view.getController().setMouseEnteredAnyChartPane(false);
            }
            
            final int b = bx(e.getX());
            
            /** mouse position really changed? */
            if (oldBMouse == b && oldYMouse == y) {
                return;
            }
            
            if (b >= 1 && b <= getNBars()) {
                setYMouse(y);
                final int row = rb(b);
                view.getController().setMouseCursorByRow(row);
            }
            
            oldBMouse = b;
            oldYMouse = y;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            mouseEntered = true;
            view.getController().setMouseEnteredAnyChartPane(true);
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            mouseEntered = false;
            view.getController().setMouseEnteredAnyChartPane(false);
        }
        
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
            //view.getController().setMouseEnteredAnyChartPane(false);
        }
    }
    
    
}
