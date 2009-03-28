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
package org.aiotrade.util.swing;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 *
 * @author Caoyuan Deng
 */
public class AIOScrollView extends JComponent {
    private final static int W_INTERSPACE = 10;
    private final static int H_INTERSPACE = 10;
    private final static int MAX_H_PER_PICTURE = 180;
    
    private JComponent viewPort;
    private List<? extends JComponent> pictures;
    
    private int dModelBegByRewind;
    
    private int hViewPort, wViewPort;
    private int nPictures, wPicture, hPicture;
    private int nRows, wRow, hRow;
    
    private boolean shouldAlign;
    
    private int shownRange;
    private int modelBeg, modelRange;
    private int modelBegBeforeRewind;
    
    private boolean frozen = false;
    
    /** <idx, x> pair */
    private Map<Integer, Integer> idxMapX = new HashMap<Integer, Integer>();
    
    public AIOScrollView(JComponent viewPort, List<? extends JComponent> pictures) {
        this.viewPort = viewPort;
        this.pictures = pictures;
        
        setOpaque(true);
        setDoubleBuffered(true);
    }
    
    /**
     * Let's image the scrolling is the model scrolling from right to left
     * and shown keeps the same. the axis origin is the scrollView's origin.
     * so, the shownBeg is always 0, and shownEnd is always shownRange
     */
    public void scrollByPixel(int nPixels) {
        if (frozen) {
            return;
        }
        
        modelBeg -= nPixels;
        
        repaint();
    }
    
    public void scrollByPicture(int nPictures) {
        if (frozen) {
            return;
        }
        
        shouldAlign = true;
        
        scrollByPixel(wRow * nPictures);
    }
    
    public void freeze() {
        this.frozen = true;
    }
    
    public void unFreeze() {
        this.frozen = false;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public void paint(Graphics g) {
        hViewPort = viewPort.getHeight();
        wViewPort = viewPort.getWidth();
        
        hPicture = (hViewPort > MAX_H_PER_PICTURE) ? MAX_H_PER_PICTURE : hViewPort;
        wPicture = (int)(hPicture * 1.382);
        
        hRow = hPicture + W_INTERSPACE;
        wRow = wPicture + H_INTERSPACE;
        nRows = hViewPort / hRow;
        if (nRows <= 0) {
            nRows = 1;
        }
        
        /**
         * keep my bounds' width same as viewPort's, but, my height could be greater
         * than viewPort's height
         */
        int hBoundsMe = hRow * nRows;
        if (hBoundsMe < hViewPort) {
            hBoundsMe = hViewPort;
        }
        setBounds(0, 0, wViewPort, hBoundsMe);
        g.setColor(getBackground());
        g.fillRect(0, 0, wViewPort, hBoundsMe);
        
        /** image that the shownRange is jointed by multiple rows */
        shownRange = wViewPort * nRows;
        
        nPictures = pictures.size();
        /** add one more picture space for scrolling */
        modelRange = wRow * (nPictures + 1);
        if (modelRange < shownRange) {
            modelRange = shownRange;
        }
        
        rewindIfNecessary();
        
        computePicturesPosition();
        
        alignIfNecessary();
        
        placePictures();
        
        super.paint(g);
    }
    
    private void rewindIfNecessary() {
        /** check if need rewind scrolling */
        int modelEnd = modelBeg + modelRange;
        int diff = modelEnd - shownRange;
        if (diff < 0) {
            /** rewind happens */
            int old = modelBeg;
            modelBeg = shownRange - diff;
            
            dModelBegByRewind = modelBeg - old;
        }
    }
    
    private void computePicturesPosition() {
        idxMapX.clear();
        for (int i = 0, n = pictures.size(); i < n; i++) {
            int x0 = modelBeg + i * wRow;
            int x0BeforeRewound = x0 - dModelBegByRewind;
            
            int x1BeforeRewound = x0BeforeRewound + wRow;
            if (x1BeforeRewound > 0 && x1BeforeRewound < modelBeg) {
                /** before rewound, it's still in view rande, so let it be shown */
                x0 = x0BeforeRewound;
            }
            
            idxMapX.put(i, x0);
        }
    }
    
    private void alignIfNecessary() {
        int pixelsScrollBack = 0;
        
        boolean noneInFront = true;
        if (shouldAlign) {
            for (Integer idx : idxMapX.keySet()) {
                int x0 = idxMapX.get(idx);
                if (x0 < 0 && x0 + wRow > 0) {
                    /** this is the current front picture, align it at 0 */
                    pixelsScrollBack = -x0;
                    noneInFront = false;
                    break;
                }
            }
            
            if (noneInFront) {
                /*-
                 * now is showing a width > wRow space, the idx0 one will be the next
                 * Do we enjoy the space for a rest? if not, just do:
                 * if (xMap.size() > 0) {
                 *     pixelsScrollBack = xMap.get(0)
                 * }
                 */
            }
            
            if (pixelsScrollBack > 0) {
                modelBeg += pixelsScrollBack;
                
                rewindIfNecessary();
                computePicturesPosition();
            }
            
            /** shouldAlign reset */
            shouldAlign = false;
        }
    }
    
    private void placePictures() {
        for (Integer idx : idxMapX.keySet()) {
            int x0 = idxMapX.get(idx);
            JComponent picture = pictures.get(idx);
            
            if (x0 + wPicture < 0 || x0 > shownRange) {
                /** not in the scope of viewRange */
                picture.setVisible(false);
                continue;
            } else {
                int rowIdx = x0 / wViewPort;
                
                /** adjust to this row's x and y */
                int xInRow = x0 - wViewPort * rowIdx;
                int yInRow = hRow * rowIdx;
                
                picture.setBounds(xInRow, yInRow, wPicture, hPicture);
                picture.setVisible(true);
            }
        }
    }
    
}

