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
package org.aiotrade.modules.ui.netbeans.windows;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import javax.swing.AbstractAction
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.view.securities.RealTimeWatchListPanel
import org.aiotrade.lib.securities.Security
import org.aiotrade.lib.securities.TickerSerProvider
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.netbeans.actions.StartSelectedWatchAction
import org.aiotrade.modules.ui.netbeans.actions.StopSelectedWatchAction
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes.SymbolStopWatchAction
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/**
 *
 * @author Caoyuan Deng
 */


/** This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @NOTE:
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 */
object RealTimeWatchListTopComponent {
  var instanceRefs = List[WeakReference[RealTimeWatchListTopComponent]]()

  // The Mode this component will live in.
  private val MODE = "editor"

  private val watchingSecs = HashSet[Security]()

  def getInstance(name: String): RealTimeWatchListTopComponent = {
    val instance = instanceRefs find (_.get.getName == name) map (_.get) getOrElse new RealTimeWatchListTopComponent(name)

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

}

import RealTimeWatchListTopComponent._
class RealTimeWatchListTopComponent private (name: String) extends TopComponent {

  private val ref = new WeakReference[RealTimeWatchListTopComponent](this)
  instanceRefs ::= ref
    
  private val tc_id = "RealTimeWatchList"
  private val symbolToNode = HashMap[String, Node]()
  private val watchListPanel = new RealTimeWatchListPanel

  private var updateServerRegistered = false
  private var reallyClosed = false

  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[StartSelectedWatchAction]))
  popup.add(SystemAction.get(classOf[StopSelectedWatchAction]))
        
  val watchListTable = watchListPanel.table
        
  // replace default "ENTER" keybinding to viewSymbolAction
  val viewSymbolAction = new AbstractAction {
    def actionPerformed(e: ActionEvent) {
      val row = watchListTable.getSelectedRow
      if (row >= 0 && row < watchListTable.getRowCount) {
        val symbol = watchListPanel.symbolAtRow(row)
        if (symbol != null) {
          symbolToNode.get(symbol) foreach {_.getLookup.lookup(classOf[ViewAction]).execute}
        }
      }
    }
  }
  watchListTable.getInputMap.put(KeyStroke.getKeyStroke("ENTER"), "viewSymbol")
  watchListTable.getActionMap.put("viewSymbol", viewSymbolAction)

  watchListTable.addMouseListener(new WatchListTableMouseListener(watchListTable, this))

  // component should setFocusable(true) to have ability to gain the focus
  setFocusable(true)

  setLayout(new BorderLayout)

  add(watchListPanel, BorderLayout.CENTER)
  setName(name)
  setBackground(LookFeel().backgroundColor)

  /** Should forward focus to sub-component watchListPanel */
  override def requestFocusInWindow: Boolean = {
    watchListPanel.requestFocusInWindow
  }

  private def showPopup(e: MouseEvent) {
    if (e.isPopupTrigger()) {
      popup.show(this, e.getX, e.getY)
    }
  }
    
  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    mode.dockInto(this)
    super.open
  }
    
  override protected def componentShowing {
    super.componentShowing
  }

  def setReallyClosed(b: Boolean) {
    this.reallyClosed = b
  }

  override protected def componentClosed {
    stopAllWatch
        
    for (ref <- instanceRefs; tc = ref.get) {
      tc.setReallyClosed(true)
      tc.close
    }
    instanceRefs = Nil
        
    super.componentClosed
  }
    
  def watch(sec: Security, node: Node) {
    watchListPanel.watch(sec.uniSymbol)
    symbolToNode.put(sec.uniSymbol, node)
    watchingSecs.add(sec)
        
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return
    }
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.addObserver(watchListPanel)
    }
  }
    
  def unWatch(sec: TickerSerProvider) {
    val uniSymbol = sec.uniSymbol
    watchListPanel.unWatch(uniSymbol)
        
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return
    }
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.deleteObserver(watchListPanel)
    }
        
    /**
     * !NOTICE
     * don't remove from tickerNodeMap, because you may need to restart it
     *    tickerNodeMap.remove(tickeringSignSeriesProvider.getSymbol());
     */
  }
    
  def getSelectedSymbolNodes: List[Node] = {
    var selectedNodes = List[Node]()
    for (row <- watchListTable.getSelectedRows()) {
      val symbol = watchListPanel.symbolAtRow(row)
      if (symbol != null) {
        symbolToNode.get(symbol) foreach {node =>
          selectedNodes ::= node
        }
      }
    }
    selectedNodes
  }
    
  private def getAllSymbolNodes: List[Node] = {
    var nodes = List[Node]()
    for (row <- 0 until watchListTable.getRowCount) {
      val symbol = watchListPanel.symbolAtRow(row)
      if (symbol != null) {
        symbolToNode.get(symbol) foreach {node =>
          nodes ::= node
        }
      }
    }
    nodes
        
  }
    
  private def stopAllWatch {
    for (node <- getAllSymbolNodes) {
      val action = node.getLookup.lookup(classOf[SymbolStopWatchAction])
      if (action != null) {
        action.execute
      }
    }
        
    for (sec <- watchingSecs) {
      val tickerServer = sec.tickerServer
      if (tickerServer != null && tickerServer.inUpdating) {
        tickerServer.stopUpdateServer
      }
    }
        
    /** should clear tickerWatchListPanel */
    watchListPanel.clearAllWatch
    watchingSecs.clear
    symbolToNode.clear
  }
    
  private class WatchListTableMouseListener(table: JTable, receiver: JComponent) extends MouseAdapter {
    private def rowAtY(e: MouseEvent): Int = {
      val colModel = table.getColumnModel
      val col = colModel.getColumnIndexAtX(e.getX)
      val row = e.getY / table.getRowHeight
            
      row
    }
        
    override def mouseClicked(e: MouseEvent) {
      showPopup(e)
    }
        
    override def mousePressed(e: MouseEvent) {
      showPopup(e)
            
      // when double click on a row, try to active this stock's realtime chart view
      if (e.getClickCount == 2) {
        val symbol = watchListPanel.symbolAtRow(rowAtY(e))
        if (symbol != null) {
          symbolToNode.get(symbol) foreach {_.getLookup.lookup(classOf[ViewAction]).execute}
        }
      }
    }
        
    override def mouseReleased(e: MouseEvent) {
      showPopup(e)
    }
  }

  override protected def preferredID: String = {
    tc_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    instanceRefs -= ref
    super.finalize
  }


  /* private def createTabbedPane {
   /** get rid of the ugly border of JTabbedPane: */
   val oldInsets = UIManager.getInsets("TabbedPane.contentBorderInsets")
   /*- set top insets as 1 for TOP placement if you want:
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0))
    */
   UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 1))
   val tabbedPane = new JTabbedPane(SwingConstants.TOP)
   UIManager.put("TabbedPane.contentBorderInsets", oldInsets)

   tabbedPane.addChangeListener(new ChangeListener() {
   private val selectedColor = new Color(177, 193, 209)

   def stateChanged(e: ChangeEvent) {
   val tp = e.getSource.asInstanceOf[JTabbedPane]

   for (i <- 0 until tp.getTabCount) {
   tp.setBackgroundAt(i, null)
   }
   val idx = tp.getSelectedIndex
   tp.setBackgroundAt(idx, selectedColor)

   updateToolbar

   tp.getSelectedComponent match {
   case viewContainer: AnalysisChartViewContainer =>
   val masterSer = viewContainer.controller.masterSer

   /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */

   val secNode_? = SymbolNodes.occupantNodeOf(contents)
   assert(secNode_?.isDefined, "There should be at least one created node bound with descriptors here, as view has been opened!")
   for (groupNode <- secNode_?.get.getChildren.getNodes) {
   groupNode.asInstanceOf[GroupNode].freq = masterSer.freq
   }

   /** update the supportedFreqsComboBox */
   //setSelectedFreqItem(masterSer.freq)
   case _ =>
   }
   }
   })

   } */

}



