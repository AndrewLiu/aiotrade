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
package org.aiotrade.platform.modules.netbeans.ui;

import java.util.logging.LogManager;
import org.openide.ErrorManager;

/**
 *
 * @author Caoyuan Deng
 */
public class NetBeansLogManager extends LogManager {
    
    ErrorManager errorManager = ErrorManager.getDefault();
    
    public NetBeansLogManager() {
    }
    
    public void log(int severity, String message) {
        errorManager.log(severity, message);
    }
    
    public void log(String message) {
        errorManager.log(message);
    }
    
    public void info(String message) {
        errorManager.log(ErrorManager.INFORMATIONAL, message);
    }
    
    public boolean isDebugEnabled() {
        return true;
    }
    
    public void debug(String message) {
        errorManager.log(ErrorManager.INFORMATIONAL, message);
    }
    
    public void debug(Throwable t) {
        errorManager.notify(ErrorManager.INFORMATIONAL, t);
    }
    
    public void error(String message) {
        errorManager.log(ErrorManager.ERROR, message);
    }
    
    public void error(Throwable t) {
        errorManager.notify(ErrorManager.ERROR, t);
    }
    
    public void notify(int severity, Throwable t) {
        errorManager.notify(severity, t);
    }
    
    public void	notify(Throwable t) {
        errorManager.notify(t);
    }
    
}
