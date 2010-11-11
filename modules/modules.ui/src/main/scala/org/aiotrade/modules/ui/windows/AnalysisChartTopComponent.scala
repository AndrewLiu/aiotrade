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
package org.aiotrade.modules.ui.windows

import java.awt.BorderLayout;
import java.awt.Image
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.logging.Logger
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.pane.DrawingPane
import org.aiotrade.lib.view.securities.AnalysisChartViewContainer
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.lib.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.modules.ui.actions.ChangeOptsAction
import org.aiotrade.modules.ui.actions.ChangeStatisticChartOptsAction
import org.aiotrade.modules.ui.actions.MarketNewsAction
import org.aiotrade.modules.ui.actions.PickIndicatorAction
import org.aiotrade.modules.ui.actions.RemoveCompareQuoteChartsAction
import org.aiotrade.modules.ui.actions.SwitchAdjustQuoteAction
import org.aiotrade.modules.ui.actions.SwitchCandleOhlcAction
import org.aiotrade.modules.ui.actions.SwitchLinearLogScaleAction
import org.aiotrade.modules.ui.actions.SwitchCalendarTradingTimeViewAction
import org.aiotrade.modules.ui.actions.SwitchHideShowDrawingLineAction
import org.aiotrade.modules.ui.actions.ZoomInAction
import org.aiotrade.modules.ui.actions.ZoomOutAction
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.modules.ui.nodes.AddToFavoriteAction
import org.openide.nodes.Node
import org.openide.util.ImageUtilities
import org.openide.util.actions.SystemAction
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager
import scala.collection.mutable.WeakHashMap


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
  private val log = Logger.getLogger(this.getClass.getName)

  private val instanceRefs = WeakHashMap[AnalysisChartTopComponent, AnyRef]()
  def instances = instanceRefs.keys


  val STANDALONE = "STANDALONE"

  private var singleton: AnalysisChartTopComponent = _

  // The Mode this component will live in.
  val MODE = "chart"

  private val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/stock.png")

  def instanceOf(symbol: String): Option[AnalysisChartTopComponent] = {
    instances find {_.sec.uniSymbol.equalsIgnoreCase(symbol)}
  }

  def apply(contents: AnalysisContents, standalone: Boolean = false): AnalysisChartTopComponent = {
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]).get
    val freq = quoteContract.freq
    
    if (standalone) {
      val instance = instances find {x =>
        x.contents == contents && x.freq == freq
      } getOrElse new AnalysisChartTopComponent(contents)

      if (!instance.isOpened) {
        instance.open
      }

      instance
    } else {
      if (singleton == null) {
        singleton = new AnalysisChartTopComponent(contents)
      }

      if ((singleton.contents ne contents) || (singleton.freq != freq)) {
        singleton.init(contents)
      }

      singleton
    }
  }

  def selected: Option[AnalysisChartTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: AnalysisChartTopComponent => Some(x)
      case _ => instances find (_.isShowing)
    }
  }
}

import AnalysisChartTopComponent._
class AnalysisChartTopComponent private ($contents: AnalysisContents) extends TopComponent with Reactor {
  instanceRefs.put(this, null)

  private var addToFavActionMenuItem: JMenuItem = _

  setFont(LookFeel().axisFont)

  private val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  splitPane.setFocusable(false)
  //splitPane.setBorder(BorderFactory.createEmptyBorder)
  splitPane.setOneTouchExpandable(true)
  splitPane.setDividerSize(3)

  // setting the resize weight to 1.0 makes the right or bottom component's size remain fixed
  splitPane.setResizeWeight(1.0)
  // to make the right component pixels wide
  splitPane.setDividerLocation(splitPane.getSize().width -
                               splitPane.getInsets.right -
                               splitPane.getDividerSize -
                               RealTimeBoardPanel.DIM.width)

  setLayout(new BorderLayout)
  add(splitPane, BorderLayout.CENTER)

  // component should setFocusable(true) to have the ability to gain the focus
  setFocusable(true)
  // Should forward focus to sub-component viewContainer
  this.addFocusListener(new FocusListener {
      def focusGained(e: FocusEvent) {
        if (viewContainer != null) {
          viewContainer.requestFocusInWindow
        }
      }

      def focusLost(e: FocusEvent) {}
    })

  class State(val contents: AnalysisContents) {
    val sec = contents.serProvider.asInstanceOf[Sec]
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]) getOrElse null
    val freq = quoteContract.freq
    val tcId = sec.secInfo.name
    val symbol = sec.uniSymbol

    val viewContainer = createViewContainer(sec, freq, contents)
    val realTimeBoard = RealTimeBoardPanel.instanceOf(sec, contents)

    splitPane.setLeftComponent(viewContainer)
    splitPane.setRightComponent(realTimeBoard)
    splitPane.revalidate

    setName(sec.secInfo.name + " - " + freq)

    private val popupMenu = new JPopupMenu
    popupMenu.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchLinearLogScaleAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchAdjustQuoteAction]))
    popupMenu.add(SystemAction.get(classOf[ZoomInAction]))
    popupMenu.add(SystemAction.get(classOf[ZoomOutAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[PickIndicatorAction]))
    popupMenu.add(SystemAction.get(classOf[ChangeOptsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[ChangeStatisticChartOptsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[MarketNewsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[RemoveCompareQuoteChartsAction]))

    SymbolNodes.findSymbolNode(contents.uniSymbol) foreach {node =>
      AnalysisChartTopComponent.this.setActivatedNodes(Array(node))
      popupMenu.add(node.getLookup.lookup(classOf[AddToFavoriteAction]))
      injectActionsToDescriptors(node)
    }

    /** inject popup menu from this TopComponent */
    viewContainer.setComponentPopupMenu(popupMenu)

    private def createViewContainer(sec: Sec, freq: TFreq, contents: AnalysisContents) = {
      val ser = sec.serOf(freq).get
      if (!ser.isLoaded) sec.loadSer(ser)

      if (SwitchAdjustQuoteAction.isAdjusted) {
        // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
        var reaction: PartialFunction[Event, Unit] = null
        reaction = {
          case TSerEvent.Loaded(ser: QuoteSer, uniSymbol, frTime, toTime, _, _) =>
            reactions -= reaction
            deafTo(ser)
            ser.adjust(true)
        }
        reactions += reaction
        listenTo(ser)
      }

      sec.subscribeTickerServer(true)

      log.info("Creating viewContainer for ser: " + System.identityHashCode(ser) + " - " + ser.freq)

      val quoteInfoSer = sec.infoPointSerOf(freq).getOrElse(null)
      if (quoteInfoSer != null && !quoteInfoSer.isLoaded) sec.loadInfoPointSer(quoteInfoSer)
      sec.subscribeQuoteInfoDataServer(true)

      val controller = ChartingController(ser, contents)
      val viewContainer = if (freq == TFreq.ONE_SEC) {
        controller.createChartViewContainer(classOf[RealTimeChartViewContainer], AnalysisChartTopComponent.this)
      } else {
        controller.createChartViewContainer(classOf[AnalysisChartViewContainer], AnalysisChartTopComponent.this)
      }

      viewContainer
    }

    private def injectActionsToDescriptors(node: Node) {
      /** we choose here to lazily create actions instances */

      /** init all children of node to create the actions that will be injected to descriptor */
      initNodeChildrenRecursively(node)
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
      val prevSec = viewContainer.controller.baseSer.serProvider.asInstanceOf[Sec]
      val currSec = contents.serProvider.asInstanceOf[Sec]
      if (currSec ne prevSec) {
        realTimeBoard.unWatch
        splitPane.remove(realTimeBoard)
        prevSec.resetSers
        contents.lookupDescriptors(classOf[IndicatorDescriptor]) foreach {_.resetInstance}
      }
      splitPane.remove(viewContainer)
    }

    state = new State(contents)
    realTimeBoard.watch

    state
  }

  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    // hidden others in "editor" mode
    /* for (tc <- mode.getTopComponents if (tc ne this) && tc.isInstanceOf[AnalysisChartTopComponent]) {
     tc.close
     } */

    /**
     * !NOTICE
     * mode.dockInto(this) seems will close this first if this.isOpened()
     * So, when call open(), try to check if it was already opened, if true,
     * no need to call open() again
     */
    mode.dockInto(this)
    super.open
  }

  /**
   * Although we have added a FocusListener to transfer the focuse to viewContainer,
   * it seems that we still need to write the following code. Don't ask me why.
   */
  override def requestFocusInWindow: Boolean = {
    viewContainer.requestFocusInWindow
  }
  
  override protected def componentActivated {
    super.componentActivated
    updateToolbar
  }
    
  override protected def componentShowing {
    super.componentShowing
  }
    
  override protected def componentClosed {
    instanceRefs.remove(this)
    realTimeBoard.unWatch
    super.componentClosed
    /**
     * componentClosed not means it will be destroied, just make it invisible,
     * so, when to call dispose() ?
     */
    //sec.setSignSeriesLoaded(false);
  }

  override def getIcon: Image = {
    iconImage
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
    super.finalize
  }
  
}
