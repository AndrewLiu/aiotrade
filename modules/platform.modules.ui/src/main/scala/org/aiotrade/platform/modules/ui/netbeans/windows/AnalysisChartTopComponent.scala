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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.ChartingControllerFactory;
import org.aiotrade.lib.charting.view.WithDrawingPane;
import org.aiotrade.lib.charting.view.pane.DrawingPane;
import org.aiotrade.lib.chartview.AnalysisChartViewContainer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.QuoteSerCombiner
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.platform.modules.ui.netbeans.NetBeansPersistenceManager;
import org.aiotrade.platform.modules.ui.netbeans.actions.ChangeOptsAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.ChangeStatisticChartOptsAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.PickIndicatorAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.RemoveCompareQuoteChartsAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.SwitchAdjustQuoteAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.SwitchCandleOhlcAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.SwitchLinearLogScaleAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.SwitchHideShowDrawingLineAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.ZoomInAction;
import org.aiotrade.platform.modules.ui.netbeans.actions.ZoomOutAction;
import org.aiotrade.platform.modules.ui.netbeans.nodes.GroupNode;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/**
 * This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * !NOTICE
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
object AnalysisChartTopComponent {
  var instanceRefs = List[WeakReference[AnalysisChartTopComponent]]()

  /** The Mode this component will live in. */
  private val MODE = "editor"

  def instanceOf(symbol: String): Option[AnalysisChartTopComponent] = {
    instanceRefs find (_.get.sec.uniSymbol.equalsIgnoreCase(symbol)) map (_.get)
  }

  def selected: Option[AnalysisChartTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: AnalysisChartTopComponent => Some(x)
      case _ => instanceRefs find (_.get.isShowing) map (_.get)
    }
  }
}

class AnalysisChartTopComponent(contents: AnalysisContents) extends TopComponent {
  import AnalysisChartTopComponent._

  private val ref = new WeakReference[AnalysisChartTopComponent](this)
  instanceRefs ::= ref

  val sec: Sec = contents.serProvider.asInstanceOf[Sec]
  private val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]) getOrElse null
  private val tc_id: String = sec.name
    
  private val symbol = sec.uniSymbol
    
  private var tabbedPaneContainer: JPanel = _
  private var tabbedPane: JTabbedPane = _
  private var supportedFreqsComboBox: JComboBox =_
    
  private var defaultViewContainer: ChartViewContainer = _
  private var freqToViewContainer = Map[TFreq, ChartViewContainer]();
    
    
  private val popupMenuForViewContainer = new JPopupMenu
    
  var weeklyCombiner: QuoteSerCombiner = _
  var monthlyCombiner: QuoteSerCombiner = _


  injectActionsToDescriptors
  injectActionsToPopupMenuForViewContainer

  loadSec
  initComponents

    
    
  private def injectActionsToDescriptors {
    /** we choose here to lazily create actions instances */
        
    /** init all children of node to create the actions that will be injected to descriptor */
//        final Node node = NetBeansPersistenceManager.getOccupantNode(contents);
//        initNodeChildrenRecursively(node);
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
    
  private def injectActionsToPopupMenuForViewContainer {
    popupMenuForViewContainer.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[SwitchLinearLogScaleAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[SwitchAdjustQuoteAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[ZoomInAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[ZoomOutAction]))
    popupMenuForViewContainer.addSeparator
    popupMenuForViewContainer.add(SystemAction.get(classOf[PickIndicatorAction]))
    popupMenuForViewContainer.add(SystemAction.get(classOf[ChangeOptsAction]))
    popupMenuForViewContainer.addSeparator
    popupMenuForViewContainer.add(SystemAction.get(classOf[ChangeStatisticChartOptsAction]))
    popupMenuForViewContainer.addSeparator
    popupMenuForViewContainer.add(SystemAction.get(classOf[RemoveCompareQuoteChartsAction]))
  }
    
  private def loadSec {
    if (!sec.isSerLoaded(quoteContract.freq)) {
      sec.loadSer(quoteContract.freq)
    }
  }

  private def initComponents {
    setFont(LookFeel().axisFont)

    setLayout(new BorderLayout)
    defaultViewContainer = addViewContainer(sec.serOf(quoteContract.freq).getOrElse(null), contents, null)
    setName(sec.name)

    /** this component should setFocusable(true) to have the ability to grab the focus */
    setFocusable(true)
    /** as the NetBeans window system manage focus in a strange manner, we should do: */
    addFocusListener(new FocusAdapter {
        override def focusGained(e: FocusEvent) {
          selectedViewContainer foreach {x =>
            x.requestFocusInWindow
          }
        }

        override def focusLost(e: FocusEvent) {
        }
      })

    //tabbedPane.setFocusable(false);
    //FocusOwnerChecker check = new FocusOwnerChecker();
  }
    
  private def initComponents_old {
    setFont(LookFeel().axisFont)
        
    createTabbedPane
    createSupportedFreqsComboBox
        
    defaultViewContainer = addViewContainer(sec.serOf(quoteContract.freq).getOrElse(null), contents, null)
        
    if (quoteContract.freq.unit == TUnit.Day) {
      createFollowedViewContainers
    }
        
    tabbedPaneContainer = new JPanel
    val overlay = new OverlayLayout(tabbedPaneContainer)
    tabbedPaneContainer.setLayout(overlay)
        
    val dim = new Dimension(80, 22)
    supportedFreqsComboBox.setPreferredSize(dim)
    supportedFreqsComboBox.setMaximumSize(dim)
    supportedFreqsComboBox.setMinimumSize(dim)
    supportedFreqsComboBox.setFocusable(false)
    /** add supportedFreqsComboBox prior to tabbedPane, so supportedFreqsComboBox can accept mouse input */
    tabbedPaneContainer.add(supportedFreqsComboBox)
        
    tabbedPane.setOpaque(false)
    tabbedPane.setFocusable(true)
    tabbedPaneContainer.add(tabbedPane)
        
    tabbedPane.setAlignmentX(1.0f)
    supportedFreqsComboBox.setAlignmentX(1.0f)
    tabbedPane.setAlignmentY(0.0f)
    supportedFreqsComboBox.setAlignmentY(0.0f)
        
    setLayout(new BorderLayout)
    add(tabbedPaneContainer, BorderLayout.CENTER)
    setName(sec.name)
        
    /** this component should setFocusable(true) to have the ability to grab the focus */
    setFocusable(true)
    /** as the NetBeans window system manage focus in a strange manner, we should do: */
    addFocusListener(new FocusAdapter {
        override def focusGained(e: FocusEvent) {
          selectedViewContainer foreach {x =>
            x.requestFocusInWindow
          }
        }
            
        override def focusLost(e: FocusEvent) {
        }
      })
        
    //tabbedPane.setFocusable(false);
    //FocusOwnerChecker check = new FocusOwnerChecker();
  }
    
  private def createTabbedPane {
    /** get rid of the ugly border of JTabbedPane: */
    val oldInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
    /*- set top insets as 1 for TOP placement if you want:
     UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
     */
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 1))
    tabbedPane = new JTabbedPane(SwingConstants.TOP)
    UIManager.put("TabbedPane.contentBorderInsets", oldInsets)
        
    tabbedPane.addChangeListener(new ChangeListener() {
        private val selectedColor = new Color(177, 193, 209)
            
        def stateChanged(e: ChangeEvent) {
          val tp = e.getSource.asInstanceOf[JTabbedPane]
                
          for (i <- 0 until tp.getTabCount) {
            tp.setBackgroundAt(i, null)
          }
          val idx = tp.getSelectedIndex
          tp.setBackgroundAt(idx, selectedColor);
                
          updateToolbar

          tp.getSelectedComponent match {
            case viewContainer: AnalysisChartViewContainer =>
              val masterSer = viewContainer.controller.masterSer
                    
              /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */
                    
              val secNode = NetBeansPersistenceManager.occupantNodeOf(contents)
              assert(secNode != null, "There should be at least one created node bound with descriptors here, as view has been opened!")
              for (groupNode <- secNode.getChildren().getNodes()) {
                groupNode.asInstanceOf[GroupNode].freq = masterSer.freq
              }
                    
              /** update the supportedFreqsComboBox */
              setSelectedFreqItem(masterSer.freq)
            case _ =>
          }
        }
      })
        
  }
    
  private def createSupportedFreqsComboBox {
    var supportedFreqs  = quoteContract.supportedFreqs
    if (supportedFreqs == null) {
      /**
       * this quoteContract supports customed frequency (as CSV), we don't
       * know how freqs it may support, so, just add the default one
       * @see QuoteSourceDescriptor#getSupportedFreqs()
       */
      supportedFreqs = Array(quoteContract.freq)
    }
        
    supportedFreqsComboBox = new JComboBox
    supportedFreqsComboBox.setModel(new DefaultComboBoxModel(supportedFreqs.asInstanceOf[Array[Object]]))
    supportedFreqsComboBox.setFocusable(false)
    supportedFreqsComboBox.setEditable(true)
    supportedFreqsComboBox.setSelectedItem(quoteContract.freq)
        
    supportedFreqsComboBox.addItemListener(new ItemListener {
        def itemStateChanged(e: ItemEvent) {
          /**
           * change a item may cause two times itemStateChanged, the old one
           * will get the ItemEvent.DESELECTED and the new item will get the
           * ItemEvent.SELECTED. so, should check the affected item first:
           */
          if (e.getStateChange != ItemEvent.SELECTED) {
            return
          }
                
          val freq = e.getItem.asInstanceOf[TFreq]
          lookupViewContainer(freq) match {
            case Some(viewContainer) =>
              selectedViewContainer_=(viewContainer)
            case None =>
              (sec.serOf(freq) match {
                  case None =>
                    val loadBeginning = sec.loadSer(freq)
                    if (loadBeginning) {
                      sec.serOf(freq)
                    } else None
                  case some => some
                }
              ) foreach {ser =>
                val viewContainer = addViewContainer(ser, contents, null)
                selectedViewContainer_=(viewContainer)
              }
          }
        }
      })
  }
    
  /**
   * !NOTICE
   * here should be aware that if sec's ser has been loaded, no more
   * SerChangeEvent.Type.FinishedLoading will be fired, so if we create followed
   * viewContainers here, should make sure that the QuoteSerCombiner listen
   * to SeriesChangeEvent.FinishingCompute or SeriesChangeEvent.FinishingLoading from
   * sec's ser and computeFrom(0) at once.
   */
  private def createFollowedViewContainers {
    val dailySer = sec.serOf(quoteContract.freq).get
    val weeklySer = new QuoteSer(TFreq.WEEKLY)
    weeklyCombiner = new QuoteSerCombiner(dailySer, weeklySer, sec.market.timeZone)
    weeklyCombiner.computeFrom(0) // don't remove me, see notice above.
    sec.putSer(weeklySer)
    addViewContainer(weeklySer, contents, null)
        
    val monthlySer = new QuoteSer(TFreq.MONTHLY)
    monthlyCombiner = new QuoteSerCombiner(dailySer, monthlySer, sec.market.timeZone)
    monthlyCombiner.computeFrom(0) // don't remove me, see notice above.
    sec.putSer(monthlySer)
    addViewContainer(monthlySer, contents, null)
        
    val tickerSer = sec.tickerSer
    if (quoteContract.isFreqSupported(tickerSer.freq)) {
      sec.loadSer(tickerSer.freq)
    }
    addViewContainer(tickerSer, contents, null)
  }
    
  private def addViewContainer(ser: QuoteSer, contents: AnalysisContents, $title: String): AnalysisChartViewContainer = {
    val controller = ChartingControllerFactory.createInstance(ser, contents)
    val viewContainer = controller.createChartViewContainer(classOf[AnalysisChartViewContainer], this).get
    val title = " " + (if ($title == null) ser.freq.name else $title) + " "
    add(viewContainer, BorderLayout.CENTER)

    freqToViewContainer += (ser.freq -> viewContainer)

    /** inject popup menu from this TopComponent */
    viewContainer.setComponentPopupMenu(popupMenuForViewContainer)

    viewContainer
  }

  private def addViewContainer_old(ser: QuoteSer, contents: AnalysisContents, $title: String): AnalysisChartViewContainer = {
    val controller = ChartingControllerFactory.createInstance(ser, contents)
    val viewContainer = controller.createChartViewContainer(classOf[AnalysisChartViewContainer], this).get
    val title = " " + (if ($title == null) ser.freq.name else $title) + " "
        
    tabbedPane.addTab(title, viewContainer)
        
    freqToViewContainer += (ser.freq -> viewContainer)
        
    /** inject popup menu from this TopComponent */
    viewContainer.setComponentPopupMenu(popupMenuForViewContainer)
        
    viewContainer
  }

  def getTabbedPane: JTabbedPane = {
    tabbedPane
  }
    
  def selectedViewContainer: Option[ChartViewContainer] = {
    //tabbedPane.getSelectedComponent match {
    //  case x: ChartViewContainer => Some(x)
    //  case _ => None
    //}
    None
  }

  def selectedViewContainer_=(viewContainer: ChartViewContainer) {
    /** check to avoid recursively call between tabbedPane and comboBox */
    selectedViewContainer match {
      case Some(x) if x eq viewContainer =>
      case _ => tabbedPane.setSelectedComponent(viewContainer)
    }
  }
    
  def setSelectedFreqItem(freq: TFreq) {
    if (supportedFreqsComboBox.getSelectedItem != freq) {
      supportedFreqsComboBox.setSelectedItem(freq)
    }
  }
    
  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    /**
     * !NOTICE
     * mode.dockInto(this) seems will close this at first if this.isOpened()
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
    sec.stopAllDataServer
        
    instanceRefs -= ref
    super.componentClosed
    /**
     * componentClosed not means it will be discarded, just make it invisible,
     * so, when to call dispose() ?
     */
    //sec.setSignSeriesLoaded(false);
  }
    
  override protected def preferredID: String = {
    if (defaultViewContainer != null) {
      defaultViewContainer.requestFocusInWindow
    }
        
    tc_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }
    
  private def updateToolbar {
    selectedViewContainer foreach {x =>
      SwitchCalendarTradingTimeViewAction.updateToolbar(x)
      SwitchAdjustQuoteAction.updateToolbar(x)
      SwitchHideShowDrawingLineAction.updateToolbar(x)
            
      x.requestFocusInWindow
    }
  }
    
  def lookupIndicator(descriptor: IndicatorDescriptor): Option[Indicator] = {
    for (viewContainer <- freqToViewContainer.valuesIterator) {
      val freq = viewContainer.controller.masterSer.freq;
      if (freq.equals(descriptor.freq)) {
        viewContainer.lookupChartView(descriptor) foreach {chartView =>
          chartView.allSers find {ser =>
            ser match {
              case ind: Indicator => ind.getClass.getName.equalsIgnoreCase(descriptor.serviceClassName)
              case _ => false
            }
          } match {
            case None =>
            case some => return some.asInstanceOf[Option[Indicator]]
          }
        }
      }
    }
        
    None
  }
    
  def lookupDrawing(descriptor: DrawingDescriptor): Option[DrawingPane] = {
    for (viewContainer <- freqToViewContainer.valuesIterator) {
      val freq = viewContainer.controller.masterSer.freq;
      if (freq.equals(descriptor.freq)) {
        val masterView = viewContainer.asInstanceOf[ChartViewContainer].masterView
                
        if (masterView.isInstanceOf[WithDrawingPane]) {
          val descriptorToDrawing = masterView.asInstanceOf[WithDrawingPane].descriptorToDrawing
                    
          return descriptorToDrawing.get(descriptor)
        }
      }
    }
        
    None
  }
    
  def lookupViewContainer(freq: TFreq): Option[ChartViewContainer] = {
    freqToViewContainer.get(freq)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    freqToViewContainer = Map()
    weeklyCombiner.dispose
    monthlyCombiner.dispose
        
    instanceRefs -= ref
    super.finalize
  }
    
  @deprecated
  private def showPopup(e: MouseEvent) {
    if (e.isPopupTrigger) {
      popupMenuForViewContainer.show(this, e.getX, e.getY)
    }
  }
    
  @deprecated 
  private class MyPopupMouseListener extends MouseListener {
    def mouseClicked(e: MouseEvent) {
      showPopup(e);
    }
    def mousePressed(e: MouseEvent) {
      showPopup(e);
    }
    def mouseReleased(e: MouseEvent) {
      showPopup(e);
    }
    def mouseEntered(e: MouseEvent) {
    }
    def mouseExited(e: MouseEvent) {
    }
  }
    
    
}
