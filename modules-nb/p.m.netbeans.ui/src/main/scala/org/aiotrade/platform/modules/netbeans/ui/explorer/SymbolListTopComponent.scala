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
package org.aiotrade.platform.modules.netbeans.ui.explorer

import java.awt.BorderLayout;
import org.aiotrade.platform.modules.netbeans.ui.explorer.SymbolNodes.SymbolNode
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor
import org.openide.windows.TopComponent;

/** 
 * Top component which displays something.
 *
 * @author Caoyuan Deng
 */
@SerialVersionUID(1L)
object SymbolListTopComponent extends TopComponent with ExplorerManager.Provider {
        
  /** holds currently scheduled/running task for set of activated node */
  private var nodeSetterTask: RequestProcessor#Task = _
  private val NODE_SETTER_LOCK = new Object
    
  private var paletteSource: TopComponent = _
    
  private val manager = new ExplorerManager
  private val treeView = new BeanTreeView
    
  private var rootNode: SymbolNode = _
  private var watchListNode: Node = _
    
  setName(NbBundle.getMessage(this.getClass, "CTL_SymbolListTopComponent"))
  setToolTipText(NbBundle.getMessage(this.getClass, "HINT_SymbolListTopComponent"))
  //setIcon(Utilities.loadImage("SET/PATH/TO/ICON/HERE", true));
        
  setLayout(new BorderLayout)
  add(treeView, BorderLayout.CENTER)
  treeView.setRootVisible(true)
  try {
    rootNode = new SymbolNodes.RootSymbolNode
    manager.setRootContext(rootNode)
  } catch {case ex: Exception => ErrorManager.getDefault.notify(ex)}
  
  val map = getActionMap
  map.put("delete", ExplorerUtils.actionDelete(manager, true))
  associateLookup(ExplorerUtils.createLookup(manager, map))
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_ALWAYS
  }
    
  override def componentOpened {
    // TODO add custom code on component opening
  }
    
  override def componentClosed {
    // TODO add custom code on component closing
  }
    
  /** replaces this in object stream */
  override def writeReplace: Object = {
    new ResolvableHelper
  }
    
  override protected def preferredID: String = {
    "SymbolListTopComponent"
  }

  def getExplorerManager: ExplorerManager = {
    manager
  }
    
  def getRootNode: SymbolNode = {
    rootNode
  }

  @serializable
  @SerialVersionUID(1L)
  class ResolvableHelper {
    def readResolve: Object = {
      this
    }
  }
}
