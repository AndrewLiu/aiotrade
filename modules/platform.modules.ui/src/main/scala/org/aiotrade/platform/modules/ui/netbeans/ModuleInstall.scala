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
package org.aiotrade.platform.modules.ui.netbeans;

//import org.aiotrade.platform.core.PersistenceManager;
//import org.aiotrade.platform.core.UserOptionsManager;

/**
 * How do i change the closing action of the MainWindow?
 * http://platform.netbeans.org/faqs/actions-how-to-add-actions-to-fi.html#DevFaqMainwindowClosingAction
 *
 * When you click the close button in the top right corner the application closes. 
 * If you want to do something before the application closes (eg. a dialog with 
 * ok and cancel options) or prevent it from closing , you'll have to override 
 * the org.openide.modules.ModuleInstall class.
 *
 * Make a class in your module that extends the org.openide.modules.ModuleInstall
 * class and override the closing() method. Keep in mind that setting the return
 * value of this method to "true" means the application will not be able to close
 * at all.
 * 
 * Next you have to tell your module to load this new class by adding a reference
 * to your Manifest file : e.g. 
 * "OpenIDE-Module-Install: org/netbeans/modules/java/JavaModule.class" 
 * where "JavaModule.class" should be the name of your class.
 *
 *
 * @author Caoyuan Deng
 */
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.util.UserOptionsManager

class ModuleInstall extends org.openide.modules.ModuleInstall {

  override protected def initialize {
    super.initialize
        
    UserOptionsManager.assertLoaded
  }

  override def closing: Boolean = {

    PersistenceManager().shutdown
        
    super.closing
  }

}
