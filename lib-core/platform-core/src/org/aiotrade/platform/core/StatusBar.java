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
package org.aiotrade.platform.core;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** 
 *
 * @author Caoyuan Deng
 */
public class StatusBar extends JPanel {
    
    // Panes in the status bar.
    private StatusPane marketPane = new StatusPane("SH");
    
    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 3));
        setBackground(Color.lightGray);
        setBorder(BorderFactory.createLineBorder(Color.darkGray));
        
        add(marketPane);
    }
    
    // Set market pane label.
    public void setMarketPane(String market) {
        
    }
    
    // Nested class defining a status bar pane.
    class StatusPane extends JLabel {
        
        private Font paneFont = new Font("Serif", Font.PLAIN, 10);
        
        public StatusPane(String text) {
            setBackground(Color.lightGray);
            //setForeground(Color.black);
            setFont(paneFont);
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            setText(text);
        }
    }
}