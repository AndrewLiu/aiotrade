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
package org.aiotrade.charting.view;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JLayeredPane;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.charting.view.pane.DrawingPane;


/**
 * This class help to implement a default drawingPart support.
 *
 *
 *
 *
 * @author Caoyuan Deng
 * @TIPS As Java does not support multi-inheriting, here shows a good solution, example:
 * QuoteChartView, which has been a sub-class of ChartView, but want some
 * Default drawing feature too. It implement WithDrawingPane interface via this
 * cookie. Actually, that is something like inhertied from ChartView and
 * WithDrawingPaneHelper.
 */
public class WithDrawingPaneHelper implements WithDrawingPane {
    private Map<DrawingDescriptor, DrawingPane> descriptorMapDrawing = 
            new HashMap<DrawingDescriptor, DrawingPane>();
    private DrawingPane selectedDrawing;
    
    private ChartView owner;
    
    public WithDrawingPaneHelper(ChartView owner) {
        this.owner = owner;
    }
    
    public DrawingPane getSelectedDrawing() {
        return selectedDrawing;
    }
    
    public void setSelectedDrawing(DrawingPane drawing) {
        selectedDrawing = drawing;
    }
    
    public DrawingDescriptor findDrawingDescriptor(DrawingPane drawing) {
        for (DrawingDescriptor descriptor : descriptorMapDrawing.keySet()) {
            DrawingPane foundDrawing = descriptorMapDrawing.get(descriptor);
            if ( foundDrawing != null && foundDrawing.equals(drawing)) {
                return descriptor;
            }
        }
        return null;
    }
    
    public void addDrawing(DrawingDescriptor descriptor, DrawingPane drawing) {
        if (descriptorMapDrawing.containsKey(descriptor)) {
            /** if this has been in drawings, don't add more */
            setSelectedDrawing(drawing);
        } else {
            descriptorMapDrawing.put(descriptor, drawing);
            
            owner.getMainLayeredPane().add(drawing, JLayeredPane.DEFAULT_LAYER);
            drawing.setVisible(false);
            owner.getMainLayeredPane().moveToBack(drawing);
            
            setSelectedDrawing(drawing);
        }
    }
    
    public void deleteDrawing(DrawingDescriptor descriptor) {
        DrawingPane drawing = descriptorMapDrawing.get(descriptor);
        if (drawing != null) {
            owner.getMainLayeredPane().remove(drawing);
            if (selectedDrawing != null && selectedDrawing.equals(drawing)) {
                selectedDrawing = null;
                owner.getController().setCursorCrossLineVisible(true);
            }
            drawing = null;
            owner.getController().updateViews();
        }
        descriptorMapDrawing.remove(descriptor);
    }
    
    public Map<DrawingDescriptor, DrawingPane> getDescriptorMapDrawing() {
        return descriptorMapDrawing;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}




