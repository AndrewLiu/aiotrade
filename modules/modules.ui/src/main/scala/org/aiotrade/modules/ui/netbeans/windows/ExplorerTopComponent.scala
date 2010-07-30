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
package org.aiotrade.modules.ui.netbeans.windows

import java.awt.BorderLayout;
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.model.Exchange
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.openide.explorer.ExplorerManager
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.loaders.DataFolder
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor
import org.openide.windows.TopComponent;

/** 
 * Top component which displays explorer tree.
 * @note This compoment must be created first to automatically monite node creating.
 * Since it will be initilize automatically by NetBeans's layer configuatrion, we need
 * to implement it as a class with public zero constructor and a companion object which
 * share an instance between them.
 *
 * @author Caoyuan Deng
 */
object ExplorerTopComponent {
  private var instance: Option[ExplorerTopComponent] = None
  def apply() = instance getOrElse new ExplorerTopComponent
}

import ExplorerTopComponent._
@serializable
@SerialVersionUID(1L)
class ExplorerTopComponent extends TopComponent with ExplorerManager.Provider {
  private val log = Logger.getLogger(this.getClass.getName)

  instance = Some(this)

  /** holds currently scheduled/running task for set of activated node */
  private val tc_id = "ExplorerTopComponent"
  
  private var nodeSetterTask: RequestProcessor#Task = _
  private val NODE_SETTER_LOCK = new Object
    
  private var paletteSource: TopComponent = _
    
  private val manager = new ExplorerManager
  private val treeView = new BeanTreeView

  val rootNode = SymbolNodes.RootSymbolsNode
  manager.setRootContext(rootNode)
    
  private var watchListNode: Node = _
    
  setName(NbBundle.getMessage(this.getClass, "CTL_ExplorerTopComponent"))
  setToolTipText(NbBundle.getMessage(this.getClass, "HINT_ExplorerTopComponent"))
  //setIcon(Utilities.loadImage("SET/PATH/TO/ICON/HERE", true));
        
  setLayout(new BorderLayout)
  add(treeView, BorderLayout.CENTER)
  treeView.setRootVisible(true)

  private val actionMap = getActionMap
  actionMap.put("delete", ExplorerUtils.actionDelete(manager, true))
  associateLookup(ExplorerUtils.createLookup(manager, actionMap))
  // --- to enable focus owner checking
  //org.aiotrade.lib.util.awt.focusOwnerChecker

  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_ALWAYS
  }
    
  override def componentOpened {
    super.componentOpened

    if (!isSymbolNodesInited) {
      log.info("Create symbols nodes from db ...")
      addSymbolsFromDB
    }
  }

  override def componentClosed {
    super.componentClosed
  }
    
  override protected def preferredID: String = {
    tc_id
  }

  def getExplorerManager: ExplorerManager = {
    manager
  }

  private def isSymbolNodesInited = {
    val rootNode = getExplorerManager.getRootContext
    rootNode.getChildren.getNodesCount > 0
  }

  private def addSymbolsFromDB {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          val rootNode = getExplorerManager.getRootContext
          val rootFolder = rootNode.getLookup.lookup(classOf[DataFolder])
          rootFolder.files.isEmpty
          // expand root node
          getExplorerManager.setExploredContext(rootNode)

          // add symbols to exchange folder
          val dailyQuoteContract  = createQuoteContract
          for (exchange <- Exchange.allExchanges;
               exchangefolder = DataFolder.create(rootFolder, exchange.code);
               symbol <- Exchange.symbolsOf(exchange)
          ) {
            dailyQuoteContract.srcSymbol = symbol
            SymbolNodes.createSymbolXmlFile(exchangefolder, symbol, dailyQuoteContract)
          }
        }
      })

  }

  private def createQuoteContract = {
    val contents = PersistenceManager().defaultContents
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get
    //quoteContract.beginDate_$eq((Date) fromDateField.getValue());
    //quoteContract.endDate_$eq((Date) toDateField.getValue());
    //quoteContract.urlString_$eq(pathField.getText().trim());

    quoteContract.freq = TFreq.DAILY

    quoteContract.refreshable = false

    quoteContract
  }
}
