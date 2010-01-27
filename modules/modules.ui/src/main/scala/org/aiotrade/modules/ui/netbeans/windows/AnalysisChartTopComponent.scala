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
import java.lang.ref.WeakReference;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartingControllerFactory
import org.aiotrade.lib.charting.view.WithDrawingPane;
import org.aiotrade.lib.charting.view.pane.DrawingPane;
import org.aiotrade.lib.view.securities.AnalysisChartViewContainer
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.securities.Security
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.modules.ui.netbeans.actions.ChangeOptsAction;
import org.aiotrade.modules.ui.netbeans.actions.ChangeStatisticChartOptsAction;
import org.aiotrade.modules.ui.netbeans.actions.PickIndicatorAction;
import org.aiotrade.modules.ui.netbeans.actions.RemoveCompareQuoteChartsAction;
import org.aiotrade.modules.ui.netbeans.actions.SwitchAdjustQuoteAction;
import org.aiotrade.modules.ui.netbeans.actions.SwitchCandleOhlcAction;
import org.aiotrade.modules.ui.netbeans.actions.SwitchLinearLogScaleAction;
import org.aiotrade.modules.ui.netbeans.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.modules.ui.netbeans.actions.SwitchHideShowDrawingLineAction;
import org.aiotrade.modules.ui.netbeans.actions.ZoomInAction;
import org.aiotrade.modules.ui.netbeans.actions.ZoomOutAction;
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager


/**
 * This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @Note
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
object AnalysisChartTopComponent {
  var instanceRefs = List[WeakReference[AnalysisChartTopComponent]]()

  private var singleton: AnalysisChartTopComponent = _

  // The Mode this component will live in.
  private val MODE = "editor"

  def instanceOf(symbol: String): Option[AnalysisChartTopComponent] = {
    instanceRefs find (_.get.sec.uniSymbol.equalsIgnoreCase(symbol)) map (_.get)
  }

  def apply(contents: AnalysisContents): AnalysisChartTopComponent = {
    apply(contents, false)
  }

  def apply(contents: AnalysisContents, standalone: Boolean): AnalysisChartTopComponent = {
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get
    val freq = quoteContract.freq
    if (standalone) {
      val instance = instanceRefs find {x => 
        x.get.contents == contents && x.get.freq == freq
      } map (_.get) getOrElse new AnalysisChartTopComponent(contents)

      if (!instance.isOpened) {
        instance.open
      }

      instance
    } else {
      if (singleton == null) {
        singleton = new AnalysisChartTopComponent(contents)
      }

      val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get
      val freq = quoteContract.freq
      if (singleton.contents != contents || singleton.freq != freq) {
        singleton.init(contents)
      }

      singleton
    }
  }

  def selected: Option[AnalysisChartTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: AnalysisChartTopComponent => Some(x)
      case _ => instanceRefs find (_.get.isShowing) map (_.get)
    }
  }
}

import AnalysisChartTopComponent._
class AnalysisChartTopComponent private ($contents: AnalysisContents) extends TopComponent {

  private val ref = new WeakReference[AnalysisChartTopComponent](this)
  instanceRefs ::= ref

  private val popupMenuForViewContainer = {
    val x = new JPopupMenu
    x.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
    x.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
    x.add(SystemAction.get(classOf[SwitchLinearLogScaleAction]))
    x.add(SystemAction.get(classOf[SwitchAdjustQuoteAction]))
    x.add(SystemAction.get(classOf[ZoomInAction]))
    x.add(SystemAction.get(classOf[ZoomOutAction]))
    x.addSeparator
    x.add(SystemAction.get(classOf[PickIndicatorAction]))
    x.add(SystemAction.get(classOf[ChangeOptsAction]))
    x.addSeparator
    x.add(SystemAction.get(classOf[ChangeStatisticChartOptsAction]))
    x.addSeparator
    x.add(SystemAction.get(classOf[RemoveCompareQuoteChartsAction]))

    x
  }

  setFont(LookFeel().axisFont)

  private val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  splitPane.setFocusable(false)
  //splitPane.setBorder(BorderFactory.createEmptyBorder)
  splitPane.setOneTouchExpandable(true)
  splitPane.setDividerSize(3)

  // setting the resize weight to 1.0 makes the right or bottom component's size remain fixed
  splitPane.setResizeWeight(1.0)

  setLayout(new BorderLayout)
  add(splitPane, BorderLayout.CENTER)

  // component should setFocusable(true) to have the ability to gain the focus
  setFocusable(true)

  class State(val contents: AnalysisContents) {
    val sec = contents.serProvider.asInstanceOf[Security]
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]) getOrElse null
    val freq = quoteContract.freq
    val tcId: String = sec.name
    val symbol = sec.uniSymbol

    val viewContainer = createViewContainer(sec, freq, contents)
    val realTimeBoard = new RealTimeBoardPanel(sec, contents)

    splitPane.add(JSplitPane.LEFT,  viewContainer)
    splitPane.add(JSplitPane.RIGHT, realTimeBoard)

    setName(sec.name + " - " + freq)

    injectActionsToDescriptors

    loadSec

    private def createViewContainer(sec: Security, freq: TFreq, contents: AnalysisContents) = {
      val ser = freq match {
        case TFreq.ONE_SEC => sec.tickerSer
        case _ => sec.serOf(freq).getOrElse(null)
      }

      val controller = ChartingControllerFactory.createInstance(ser, contents)
      val viewContainer = if (freq == TFreq.ONE_SEC) {
        controller.createChartViewContainer(classOf[RealTimeChartViewContainer], AnalysisChartTopComponent.this)
      } else {
        controller.createChartViewContainer(classOf[AnalysisChartViewContainer], AnalysisChartTopComponent.this)
      }

      /** inject popup menu from this TopComponent */
      viewContainer.setComponentPopupMenu(popupMenuForViewContainer)

      viewContainer
    }

    private def injectActionsToDescriptors {
      /** we choose here to lazily create actions instances */

      /** init all children of node to create the actions that will be injected to descriptor */
      SymbolNodes.occupantNodeOf(contents) foreach (initNodeChildrenRecursively(_))
    }

    private def initNodeChildrenRecursively(node: Node) {
      if (!node.isLeaf) {
        /** call getChildren().getNodes(true) to initialize all children nodes */
        val childrenNodes = node.getChildren.getNodes(true)
        for (child <- childrenNodes) {
          initNodeChildrenRecursively(child)
        }
      }
    }

    private def loadSec {
      if (!sec.isSerLoaded(quoteContract.freq)) {
        sec.loadSer(quoteContract.freq)
      }
      sec.subscribeTickerServer
    }

  }

  private var state = init($contents)
  def contents = state.contents
  def viewContainer = state.viewContainer
  def realTimeBoard = state.realTimeBoard
  def freq = state.freq

  private def sec = state.sec
  private def tcId = state.tcId

  def init(contents: AnalysisContents): State = {
    if (state != null) {
      unWatch
    }

    state = new State(contents)
    watch

    state
  }

  def watch {
    val tickerServer = sec.tickerServer
    if (tickerServer != null) {
      tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
        tickerSnapshot.addObserver(realTimeBoard)
      }
    }
  }

  def unWatch {
    val tickerServer = sec.tickerServer
    if (tickerServer != null) {
      tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
        tickerSnapshot.deleteObserver(realTimeBoard)
      }
    }
  }

  /** Should forward focus to sub-component viewContainer */
  override def requestFocusInWindow: Boolean = {
    viewContainer.requestFocusInWindow
  }

  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    // hidden others in "editor" mode
    for (tc <- mode.getTopComponents if (tc ne this) && tc.isInstanceOf[AnalysisChartTopComponent]) {
      tc.close
    }

    /**
     * !NOTICE
     * mode.dockInto(this) seems will close this first if this.isOpened()
     * So, when call open(), try to check if it was already opened, if true,
     * no need to call open() again
     */
    mode.dockInto(this)
    super.open
  }
    
  override protected def componentActivated {
    super.componentActivated
    updateToolbar
  }
    
  override protected def componentShowing {
    super.componentShowing
  }
    
  override protected def componentClosed {
    instanceRefs -= ref
    unWatch
    super.componentClosed
    /**
     * componentClosed not means it will be destroied, just make it invisible,
     * so, when to call dispose() ?
     */
    //sec.setSignSeriesLoaded(false);
  }
    
  override protected def preferredID: String = {
    tcId
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }
    
  private def updateToolbar {
    SwitchCalendarTradingTimeViewAction.updateToolbar(viewContainer)
    SwitchAdjustQuoteAction.updateToolbar(viewContainer)
    SwitchHideShowDrawingLineAction.updateToolbar(viewContainer)
            
    viewContainer.requestFocusInWindow
  }
    
  def lookupIndicator(descriptor: IndicatorDescriptor): Option[Indicator] = {
    val a = viewContainer.lookupChartView(descriptor) foreach {chartView =>
      chartView.allSers find {_.getClass.getName.equalsIgnoreCase(descriptor.serviceClassName)} match {
        case None =>
        case some => return some.asInstanceOf[Option[Indicator]]
      }
    }
    
    None
  }
    
  def lookupDrawing(descriptor: DrawingDescriptor): Option[DrawingPane] = {
    viewContainer.masterView match {
      case drawingPane: WithDrawingPane => drawingPane.descriptorToDrawing.get(descriptor)
      case _ => None
    }
  }
    
  @throws(classOf[Throwable])
  override protected def finalize {
    instanceRefs -= ref
    super.finalize
  }
  
}
