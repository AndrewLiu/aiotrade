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
package org.aiotrade.platform.modules.ui.netbeans.windows;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import org.aiotrade.platform.modules.ui.netbeans.nodes.SymbolNodes.SymbolStartWatchAction
import org.aiotrade.platform.modules.ui.netbeans.nodes.SymbolNodes.SymbolStopWatchAction
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.chartview.RealTimeWatchListPanel
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.TickerSerProvider
import org.aiotrade.platform.modules.ui.netbeans.actions.StartSelectedWatchAction
import org.aiotrade.platform.modules.ui.netbeans.actions.StopSelectedWatchAction
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
  /** The Mode this component will live in. */
  private val MODE = "editor"
  var instanceRefs = List[WeakReference[RealTimeWatchListTopComponent]]()

  private val watchingSecs = HashSet[Sec]()

  def getInstance: RealTimeWatchListTopComponent = {
    val instance = if (instanceRefs.isEmpty) new RealTimeWatchListTopComponent else instanceRefs.head.get

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

}

class RealTimeWatchListTopComponent extends TopComponent {
  import RealTimeWatchListTopComponent._

  private var ref = new WeakReference[RealTimeWatchListTopComponent](this)
  instanceRefs ::= ref
    
  private val tc_id = "RealTimeWatchList"
    
  private var updateServerRegistered = false
    
  private var reallyClosed = false
  
  private val symbolToNode = HashMap[String, Node]()
    
  private val rtWatchListPanel = new RealTimeWatchListPanel

  setLayout(new BorderLayout)
        
  add(rtWatchListPanel, BorderLayout.CENTER)
  setName("List")
  setBackground(LookFeel().backgroundColor)
        
  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[StartSelectedWatchAction]))
  popup.add(SystemAction.get(classOf[StopSelectedWatchAction]))
        
  val watchListTable = rtWatchListPanel.getWatchListTable
        
  watchListTable.addMouseListener(new WatchListTableMouseListener(watchListTable, this))
    
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
        
    for (ref <- instanceRefs) {
      ref.get.setReallyClosed(true)
      ref.get.close
    }
    instanceRefs = Nil
        
    super.componentClosed
  }
    
  def watch(sec: Sec, node: Node) {
    rtWatchListPanel.watch(sec.uniSymbol)
    symbolToNode.put(sec.uniSymbol, node)
    watchingSecs.add(sec)
        
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return
    }
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.addObserver(rtWatchListPanel)
    }
  }
    
  def unWatch(sec: TickerSerProvider) {
    val uniSymbol = sec.uniSymbol
    rtWatchListPanel.unWatch(uniSymbol)
        
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return
    }
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.deleteObserver(rtWatchListPanel)
    }
        
    /**
     * !NOTICE
     * don't remove from tickerNodeMap, because you may need to restart it
     *    tickerNodeMap.remove(tickeringSignSeriesProvider.getSymbol());
     */
  }
    
  def getSelectedSymbolNodes: List[Node] = {
    var selectedNodes = List[Node]();
    for (row <- rtWatchListPanel.getWatchListTable.getSelectedRows()) {
      val symbol = rtWatchListPanel.symbolAtRow(row)
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
    for (row <- 0 until rtWatchListPanel.getWatchListTable.getRowCount) {
      val symbol = rtWatchListPanel.symbolAtRow(row)
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
    rtWatchListPanel.clearAllWatch
    watchingSecs.clear
    symbolToNode.clear
  }
    
  private class WatchListTableMouseListener(table: JTable, receiver: JComponent) extends MouseListener {
        
    private def rowAtY(e: MouseEvent): Int = {
      val columnModel = table.getColumnModel
      val col = columnModel.getColumnIndexAtX(e.getX)
      val row = e.getY / table.getRowHeight
            
      row
    }
        
    def mouseClicked(e: MouseEvent) {
      showPopup(e)
    }
        
    def mousePressed(e: MouseEvent) {
      showPopup(e)
            
      /** when double click on a row, try to active this stock's tickering chart view */
      if (e.getClickCount == 2) {
        val symbol = rtWatchListPanel.symbolAtRow(rowAtY(e))
        if (symbol == null) {
          return
        }
                
        symbolToNode.get(symbol) foreach {node =>
          val watchAction = node.getLookup.lookup(classOf[SymbolStartWatchAction])
          if (watchAction != null) {
            watchAction.execute
          }
        }
      }
    }
        
    def mouseReleased(e: MouseEvent) {
      showPopup(e)
    }
        
    def mouseEntered(e: MouseEvent) {
    }
        
    def mouseExited(e: MouseEvent) {
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
    
}



