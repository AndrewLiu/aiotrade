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
import java.awt.Image
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane
import javax.swing.JTable;
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.view.securities.RealTimeWatchListPanel
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.netbeans.actions.OpenMultipleChartsAction
import org.aiotrade.modules.ui.netbeans.actions.StartSelectedWatchAction
import org.aiotrade.modules.ui.netbeans.actions.StopSelectedWatchAction
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes.SymbolStopWatchAction
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap

/** This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @NOTE:
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
object RealTimeWatchListTopComponent {
  private val instanceRefs = WeakHashMap[RealTimeWatchListTopComponent, AnyRef]()
  def instances = instanceRefs.keys

  // The Mode this component will live in.
  private val MODE = "info"

  private val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/netbeans/resources/market.png")

  private val watchingSecs = HashSet[Sec]()

  def getInstance(name: String): RealTimeWatchListTopComponent = {
    val instance = instances find (_.name == name) getOrElse new RealTimeWatchListTopComponent(name)

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

  def selected: Option[RealTimeWatchListTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: RealTimeWatchListTopComponent => Some(x)
      case _ => instances find (_.isShowing)
    }
  }

}

import RealTimeWatchListTopComponent._
class RealTimeWatchListTopComponent private (val name: String) extends TopComponent {
  instanceRefs.put(this, null)
    
  private val tc_id = "RealTimeWatchList"
  private val watchListPanel = new RealTimeWatchListPanel
  private var realTimeBoard: RealTimeBoardPanel = _

  private var updateServerRegistered = false
  private var reallyClosed = false

  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[OpenMultipleChartsAction]))
  popup.add(SystemAction.get(classOf[StartSelectedWatchAction]))
  popup.add(SystemAction.get(classOf[StopSelectedWatchAction]))

  private val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  splitPane.setFocusable(false)
  //splitPane.setBorder(BorderFactory.createEmptyBorder)
  splitPane.setOneTouchExpandable(true)
  splitPane.setDividerSize(3)

  // setting the resize weight to 1.0 makes the right or bottom component's size remain fixed
  splitPane.setResizeWeight(1.0)

  setLayout(new BorderLayout)
  add(splitPane, BorderLayout.CENTER)

  setName(name)
  setBackground(LookFeel().backgroundColor)

  // component should setFocusable(true) to have the ability to gain the focus
  setFocusable(true)

  val watchListTable = watchListPanel.table
        
  // replace default "ENTER" keybinding to viewSymbolAction
  val viewSymbolAction = new AbstractAction {
    def actionPerformed(e: ActionEvent) {
      val row = watchListTable.getSelectedRow
      if (row >= 0 && row < watchListTable.getRowCount) {
        val symbol = watchListPanel.symbolAtRow(row)
        if (symbol != null) {
          SymbolNodes.findSymbolNode(symbol) foreach {_.getLookup.lookup(classOf[ViewAction]).execute}
        }
      }
    }
  }
  watchListTable.getInputMap.put(KeyStroke.getKeyStroke("ENTER"), "viewSymbol")
  watchListTable.getActionMap.put("viewSymbol", viewSymbolAction)

  watchListTable.addMouseListener(new WatchListTableMouseListener(watchListTable, this))

  watchListTable.getSelectionModel.addListSelectionListener(new ListSelectionListener {
      def valueChanged(e: ListSelectionEvent) {
        val lsm = e.getSource.asInstanceOf[ListSelectionModel]
        if (lsm.isSelectionEmpty) {
          //no rows are selected
        } else {
          val row = watchListTable.getSelectedRow
          if (row >= 0 && row < watchListTable.getRowCount) {
            val symbol = watchListPanel.symbolAtRow(row)
            if (symbol != null) {
              SymbolNodes.findSymbolNode(symbol) foreach {x =>
                val viewAction = x.getLookup.lookup(classOf[ViewAction])
                viewAction.putValue(AnalysisChartTopComponent.STANDALONE, false)
                viewAction.execute
              }
              
//              for (node <- symbolToNode.get(symbol)) {
//                val contents = node.getLookup.lookup(classOf[AnalysisContents]);
//                val sec = contents.serProvider.asInstanceOf[Sec]
//
//                if (realTimeBoard != null) {
//                  realTimeBoard.unWatch
//                  splitPane.remove(realTimeBoard)
//                }
//                realTimeBoard = RealTimeBoardPanel.instanceOf(sec, contents)
//                realTimeBoard.watch
//
//                splitPane.setRightComponent(realTimeBoard)
//                splitPane.revalidate
//              }
            }
          }
        }
      }
    })

  splitPane.setLeftComponent(watchListPanel)

  /** Should forward focus to sub-component watchListPanel */
  override def requestFocusInWindow: Boolean = {
    watchListPanel.requestFocusInWindow
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
    setReallyClosed(true)
    super.componentClosed
  }


  override def getIcon: Image = {
    iconImage
  }

  def watch(sec: Sec, node: Node) {
    watchListPanel.watch(sec)
    watchingSecs.add(sec)
  }
    
  def unWatch(sec: Sec) {
    watchListPanel.unWatch(sec)
  }
    
  def getSelectedSymbolNodes: List[Node] = {
    var selectedNodes = List[Node]()
    for (row <- watchListTable.getSelectedRows) {
      val symbol = watchListPanel.symbolAtRow(row)
      if (symbol != null) {
        SymbolNodes.findSymbolNode(symbol) foreach {node =>
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
        SymbolNodes.findSymbolNode(symbol) foreach {node =>
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
      sec.unSubscribeTickerServer
    }
        
    /** should clear tickerWatchListPanel */
    watchListPanel.clearAllWatch
    watchingSecs.clear
  }

  private class WatchListTableMouseListener(table: JTable, receiver: JComponent) extends MouseAdapter {
    private def showPopup(e: MouseEvent) {
      if (e.isPopupTrigger) {
        popup.show(RealTimeWatchListTopComponent.this, e.getX, e.getY)
      }
    }

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
            
      // when double click on a row, active this stock's realtime chart view
      if (e.getClickCount == 2) {
        val symbol = watchListPanel.symbolAtRow(rowAtY(e))
        if (symbol != null) {
          SymbolNodes.findSymbolNode(symbol) foreach {x =>
            val viewAction = x.getLookup.lookup(classOf[ViewAction])
            viewAction.putValue(AnalysisChartTopComponent.STANDALONE, true)
            viewAction.execute
          }
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
   val baseSer = viewContainer.controller.baseSer

   /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */

   val secNode_? = SymbolNodes.occupantNodeOf(contents)
   assert(secNode_?.isDefined, "There should be at least one created node bound with descriptors here, as view has been opened!")
   for (groupNode <- secNode_?.get.getChildren.getNodes) {
   groupNode.asInstanceOf[GroupNode].freq = baseSer.freq
   }

   /** update the supportedFreqsComboBox */
   //setSelectedFreqItem(baseSer.freq)
   case _ =>
   }
   }
   })

   } */

}



