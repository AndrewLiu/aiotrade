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
package org.aiotrade.platform.core.ui.netbeans.actions;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import org.aiotrade.lib.charting.view.ChartingControllerFactory;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class SwitchCursorAcceleratedAction extends CallableSystemAction {
    
    private static JToggleButton toggleButton;
    
    /** Creates a new instance of ZoomInAction
     */
    public SwitchCursorAcceleratedAction() {
    }
    
    /**
     * @NOTICE
     * If override getToolbarPresenter(), you should process action in that
     * component instead of here.
     */
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (toggleButton.isSelected()) {
                    toggleButton.setSelected(false);
                } else {
                    toggleButton.setSelected(true);
                }
            }
        });
        
    }
    
    public static boolean isCursorAccelerated() {
        return toggleButton.isSelected();
    }
    
    public String getName() {
        return "Accelerate Cursor Moving";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected String iconResource() {
        return "org/aiotrade/platform/core/ui/netbeans/resources/switchCursorAcceleratedAction.gif";
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    public static void setSelected(boolean b) {
        toggleButton.setSelected(b);
    }
    
    @Override
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/switchCursorAcceleratedAction.gif");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Fast Moving");
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final int state = e.getStateChange();
                
                if (state == ItemEvent.SELECTED) {
                    ChartingControllerFactory.setCursorAccelerated(true);
                } else {
                    ChartingControllerFactory.setCursorAccelerated(false);
                }
            }
        });
        
        return toggleButton;
    }
    
    
}


