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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.charting.view.pane.Pane.RenderStrategy;
import org.aiotrade.charting.widget.PathWidget;



/**
 *
 * @author Caoyuan Deng
 */
public class DivisionPane extends Pane {

    private MyMouseAdapter myMouseAdapter;
    
    public DivisionPane(ChartView view, DatumPlane datumPlane) {
        super(view, datumPlane);
        
        setOpaque(true);
        setRenderStrategy(RenderStrategy.NoneBuffer);
        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        
        myMouseAdapter = new MyMouseAdapter();
        addMouseMotionListener(myMouseAdapter);
        addMouseListener(myMouseAdapter);
    }

    /** 
     * @Notice:
     * It seems horizontal or vertical line won't be painted by widger#renter because of not intersect.
     * But, I'll leave it as it, since I won't draw a real line now. We'll process border by chart pane itself.
     */
    protected void plotPane_old() {
        PathWidget pathWidget = addWidget(new PathWidget());
        pathWidget.setForeground(LookFeel.getCurrent().borderColor);
        GeneralPath path = pathWidget.getPath();
        path.reset();
        
        path.moveTo(0, 0);
        path.lineTo(getWidth(), 0);
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (myMouseAdapter != null) {
            removeMouseListener(myMouseAdapter);
            removeMouseMotionListener(myMouseAdapter);
        }
        
        super.finalize();
    }
    
    private class MyMouseAdapter extends MouseAdapter implements MouseMotionListener {
        private boolean readyToDrag = false;
        private int yMousePressed;
        
        @Override
        public void mousePressed(MouseEvent e) {
            yMousePressed = e.getY();
            
            readyToDrag = true;
        }
        
        public void mouseDragged(MouseEvent e) {
            if (!(view.getParent() instanceof ChartViewContainer)) {
                return;
            }
            
            ChartViewContainer viewContainer = (ChartViewContainer)view.getParent();
            if (readyToDrag) {
                /** as this pane only 1 point height, the yMoved will always be e.getY() */
                int yMoved = e.getY();
                viewContainer.adjustViewsHeight(yMoved);
            }
            
            yMousePressed = e.getY();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            readyToDrag = false;
        }
        
        public void mouseMoved(MouseEvent e) {
        }
    }
    
}
