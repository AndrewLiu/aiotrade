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
package org.aiotrade.platform.modules.dataserver.ib.netbeans.actions;

import java.awt.Component;
import javax.swing.JOptionPane;
import TestJavaClient.SampleFrame;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;


/**
 *
 * @author Caoyuan Deng
 */
public class IBConnectAction extends CallableSystemAction {
    
    /** Creates a new instance
     */
    public IBConnectAction() {
    }
    
    
    public void performAction() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    act();
                }
            });
        } catch (Exception e) {
        }
        
    }
    
    private void act() {
        SampleFrame sampleFrame = new SampleFrame();
        sampleFrame.setVisible(true);
    }
    
    static public void inform( Component parent, String str) {
        showMsg( parent, str, JOptionPane.INFORMATION_MESSAGE);
    }
    
    static private void showMsg( Component parent, String str, int type) {
        // this function pops up a dlg box displaying a message
        JOptionPane.showMessageDialog( parent, str, "IB Java Test Client", type);
    }
    
    public String getName() {
        return "IB";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    //    protected String iconResource() {
    //        return "com/aiotrade/platform/core/netbeans/resources/switchScale.png";
    //    }
    
    protected boolean asynchronous() {
        return false;
    }
    
    
}





