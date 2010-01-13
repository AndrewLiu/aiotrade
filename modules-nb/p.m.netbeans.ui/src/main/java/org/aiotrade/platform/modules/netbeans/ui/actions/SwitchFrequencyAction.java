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
package org.aiotrade.platform.modules.netbeans.ui.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.aiotrade.lib.math.timeseries.MasterSer;
import org.aiotrade.lib.math.timeseries.Unit;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class SwitchFrequencyAction extends CallableSystemAction {
    
    private static JToggleButton toggleButton;
    private static ButtonGroup buttonGroup;
    private static JPopupMenu popup;
    private MenuItemListener menuItemListener;
    
    public SwitchFrequencyAction() {
    }
    
    
    public void performAction() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            }
        });
        
    }
    
    public String getName() {
        return "Switch Frequency";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    public Component getToolbarPresenter() {
        Image iconImage = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/dropdown.png");
        ImageIcon icon = new ImageIcon(iconImage);
        
        toggleButton = new JToggleButton();
        
        toggleButton.setForeground(Color.BLUE);
        toggleButton.setFont(new Font("Courier New", Font.ITALIC, 15));
        toggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
        toggleButton.setText("1d");
        toggleButton.setIcon(icon);
        toggleButton.setToolTipText("Switch Frequency");
        
        popup = new JPopupMenu();
        menuItemListener = new MenuItemListener();
        
        buttonGroup = new ButtonGroup();
        
        for (Unit unit : Unit.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(unit.toString());
            if (unit == Unit.Day) {
                item.setSelected(true);
            }
            item.addActionListener(menuItemListener);
            buttonGroup.add(item);
            popup.add(item);
        }
        
        toggleButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int state = e.getStateChange();
                if (state == ItemEvent.SELECTED) {
                    /** show popup menu on toggleButton at position: (0, height) */
                    popup.show(toggleButton, 0, toggleButton.getHeight());
                }
            }
        });
        
        popup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
                toggleButton.setSelected(false);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                toggleButton.setSelected(false);
            }
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        
        return toggleButton;
    }
    
    public static void setSelectedItem(MasterSer masterSignSeries) {
//        AbstractSignSeries.CombinedFrequency combinedFrequency = masterSignSeries.getCombinedFrequency();
//        
//        for (MenuElement item : popup.getSubElements()) {
//            if (item instanceof JRadioButtonMenuItem) {
//                if (((JRadioButtonMenuItem)item).getText().equalsIgnoreCase(combinedFrequency.toString())) {
//                    ((JRadioButtonMenuItem)item).setSelected(true);
//                    
//                    updateToggleButtonText(combinedFrequency, masterSignSeries);
//                }
//            }
//        }
    }
    
    private static void updateToggleButtonText(Unit unit, MasterSer masterSer) {
        
    }
    
    private class MenuItemListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            
        }
    }
    
}


