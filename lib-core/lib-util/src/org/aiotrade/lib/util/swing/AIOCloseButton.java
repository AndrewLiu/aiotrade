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
package org.aiotrade.lib.util.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 *
 * @author Caoyuan Deng
 */
public class AIOCloseButton extends JButton {
    private final static int ICON_SIZE = 12;
    private Icon chosenIcon;
    private Icon unchosenIcon;
    private Icon chosenRolloverIcon;
    private Icon unchosenRolloverIcon;
    
    private boolean chosen;
    
    public AIOCloseButton() {
        super();
        setFocusPainted(false);
        setRolloverEnabled(true);
        setBorderPainted(false);
        setContentAreaFilled(false);
        
        setIcons();
    }
    
    private void setIcons() {
        if (getForeground() == null) {
            return;
        }
        
        final int wMark = ICON_SIZE - 3;
        final int hMark = ICON_SIZE - 3;
        
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(getForeground());
        g.drawRect(1, 1, wMark, hMark);
        g.dispose();
        chosenIcon = new ImageIcon(image);
        
        image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        unchosenIcon = new ImageIcon(image);
        
        image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setColor(getForeground());
        g.drawRect(1, 1, wMark, hMark);
        g.drawLine(3, 3, wMark - 1, hMark - 1);
        g.drawLine(3, wMark - 1, hMark - 1, 3);
        g.dispose();
        chosenRolloverIcon = new ImageIcon(image);
        
        image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setColor(getForeground());
        g.drawLine(3, 3, wMark - 1, hMark - 1);
        g.drawLine(3, wMark - 1, hMark - 1, 3);
        g.dispose();
        unchosenRolloverIcon = new ImageIcon(image);
        
        setIcon(isChosen() ? chosenIcon : unchosenIcon);
        setRolloverIcon(isChosen() ? chosenRolloverIcon : unchosenRolloverIcon);
    }
    
    @Override
    public void setForeground(Color fg) {
        final Color oldValue = getForeground();
        super.setForeground(fg);
        if (oldValue == null || ! oldValue.equals(fg)) {
            setIcons();
        }
    }
    
    public boolean isChosen() {
        return chosen;
    }
    
    public void setChosen(boolean b) {
        final boolean oldValue = isChosen();
        this.chosen = b;
        if (oldValue != b) {
            setIcon(isChosen() ? chosenIcon : unchosenIcon);
            setRolloverIcon(isChosen() ? chosenRolloverIcon : unchosenRolloverIcon);
        }
    }
    
}


