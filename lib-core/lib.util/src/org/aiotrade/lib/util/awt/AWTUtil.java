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
package org.aiotrade.lib.util.awt;

import java.awt.Component;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

/**
 *
 * @author Caoyuan Deng
 */
public class AWTUtil {
    
    public final static void removeAllAWTListenersOf(Component c) {
        for (MouseListener l : c.getMouseListeners()) {
            c.removeMouseListener(l);
        }

        for (MouseMotionListener l : c.getMouseMotionListeners()) {
            c.removeMouseMotionListener(l);
        }
        
        for (MouseWheelListener l : c.getMouseWheelListeners()) {
            c.removeMouseWheelListener(l);
        }
        
        for (KeyListener l : c.getKeyListeners()) {
            c.removeKeyListener(l);
        }
        
        for (ComponentListener l : c.getComponentListeners()) {
            c.removeComponentListener(l);
        }
        
        for (InputMethodListener l : c.getInputMethodListeners()) {
            c.removeInputMethodListener(l);
        }
        
        for (FocusListener l : c.getFocusListeners()) {
            c.removeFocusListener(l);
        }
    }
}



