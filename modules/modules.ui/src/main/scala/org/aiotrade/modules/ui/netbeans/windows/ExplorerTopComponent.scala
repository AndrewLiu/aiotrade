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

import java.awt.BorderLayout
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.model.Exchange
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.text.DefaultEditorKit
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.progress.ProgressUtils
import org.openide.explorer.ExplorerManager
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.loaders.DataFolder
import org.openide.nodes.Node;
import org.openide.util.Lookup
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor
import org.openide.windows.TopComponent
import scala.collection.mutable.HashMap


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
class ExplorerTopComponent extends TopComponent with ExplorerManager.Provider with Lookup.Provider {
  private val log = Logger.getLogger(this.getClass.getName)

  instance = Some(this)

  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.netbeans.windows.Bundle")

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
  actionMap.put(DefaultEditorKit.copyAction, ExplorerUtils.actionCopy(manager))
  actionMap.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(manager))
  actionMap.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(manager))
  actionMap.put("delete", ExplorerUtils.actionDelete(manager, true)) // or false

  // following line tells the top component which lookup should be associated with it
  associateLookup(ExplorerUtils.createLookup(manager, actionMap))
  
  // --- to enable focus owner checking
  org.aiotrade.lib.util.awt.focusOwnerChecker

  private var isSymbolNodesInited = false

  def getExplorerManager: ExplorerManager = manager

  // It is good idea to switch all listeners on and off when the
  // component is shown or hidden. In the case of TopComponent use:
  override def componentActivated {
    super.componentActivated
    ExplorerUtils.activateActions(manager, true)
  }
  override def componentDeactivated {
    ExplorerUtils.activateActions(manager, false)
    super.componentDeactivated
  }

  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_ALWAYS
  }
    
  override def componentOpened {
    super.componentOpened

    if (!isSymbolNodesAdded) {
      val handle = ProgressHandleFactory.createHandle(Bundle.getString("MSG_CreateSymbolNodes"))
      ProgressUtils.showProgressDialogAndRun(new Runnable {
          def run {
            addSymbolsFromDB(handle)
          }
        }, handle, false)
    }

    scala.actors.Actor.actor {SymbolNodes.openAllSymbolFolders}
  }

  override def componentClosed {
    super.componentClosed
  }
    
  override protected def preferredID: String = {
    tc_id
  }

  override def getDisplayName: String = Bundle.getString("CTL_ExplorerTopComponent")

  private def isSymbolNodesAdded = {
    val rootNode = getExplorerManager.getRootContext
    rootNode.getChildren.getNodesCount > 0
  }

  private def addSymbolsFromDB(handle: ProgressHandle) {
    val rootNode = getExplorerManager.getRootContext
    val rootFolder = rootNode.getLookup.lookup(classOf[DataFolder])

    // expand root node
    getExplorerManager.setExploredContext(rootNode)

    DataFolder.create(rootFolder, SymbolNodes.favoriteFolderName)

    val start = System.currentTimeMillis
    log.info("Create symbols node files from db ...")
    
    // add symbols to exchange folder
    val dailyQuoteContract = createQuoteContract
    val symbolsToFolder = new HashMap[String, DataFolder]
    for (exchange <- Exchange.allExchanges;
         exchangeFolder = DataFolder.create(rootFolder, exchange.code);
         symbol <- Exchange.symbolsOf(exchange)
    ) {
      symbolsToFolder.put(symbol, exchangeFolder)
    }
    
    handle.switchToDeterminate(symbolsToFolder.size)
    var i = 0
    val allSymbols = symbolsToFolder.iterator
    while (allSymbols.hasNext) {
      handle.progress(i)
      val (symbol, folder) = allSymbols.next
      dailyQuoteContract.srcSymbol = symbol
      SymbolNodes.createSymbolXmlFile(folder, symbol, dailyQuoteContract)
      i += 1
    }

    handle.finish
    log.info("Created symbols node files from db in " + ((System.currentTimeMillis - start) / 1000.0) + "s")
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
