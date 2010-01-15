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
package org.aiotrade.platform.modules.netbeans.ui.windows;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants
import org.aiotrade.lib.charting.view.ChartingControllerFactory;
import org.aiotrade.lib.chartview.RealTimeChartViewContainer
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.modules.netbeans.ui.actions.SwitchCandleOhlcAction;
import org.aiotrade.lib.securities.Stock
import org.aiotrade.platform.modules.netbeans.ui.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.ZoomInAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.ZoomOutAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
/**
 *
 * @author Caoyuan Deng
 */


/**
 * This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @NOTICE:
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 */
object RealTimeChartTopComponent {
  var instanceRefs = List[WeakReference[RealTimeChartTopComponent]]()

  /** The Mode this component will live in. */
  private val MODE = "realtime_";
  private val REALTIME_MODE_NUMBER = 3


  def getInstance(stock: Stock, contents: AnalysisContents): RealTimeChartTopComponent = {
    val instance = instanceRefs find (x => x.get.getStock == stock) map (_.get) getOrElse new RealTimeChartTopComponent(stock, contents)
    
    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

}
class RealTimeChartTopComponent(stock: Stock, contents: AnalysisContents) extends TopComponent {
  import RealTimeChartTopComponent._

  private val ref = new WeakReference[RealTimeChartTopComponent](this)
  instanceRefs ::= ref
    
  private val symbol = stock.uniSymbol
  private val s_id = stock.name + "_RT"
        
  //        if (!stock.isSeriesLoaded()) {
  //            stock.loadSeries();
  //        }
        
  private val controller = ChartingControllerFactory.createInstance(stock.tickerSer, contents)
  private val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], this).get
        
  setLayout(new BorderLayout)
        
  add(viewContainer, SwingConstants.CENTER)
  setName(stock.name + "_RT")
        
  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
  popup.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
  popup.add(SystemAction.get(classOf[ZoomInAction]))
  popup.add(SystemAction.get(classOf[ZoomOutAction]))
        
  addMouseListener(new MouseListener {
      def mouseClicked(e: MouseEvent) {
        showPopup(e)
      }
      def mousePressed(e: MouseEvent) {
        showPopup(e)
      }
      def mouseReleased(e: MouseEvent) {
        showPopup(e)
      }
      def mouseEntered(e: MouseEvent) {
      }
      def mouseExited(e: MouseEvent) {
      }
    })
        
  /** this component should setFocusable(true) to have the ability to grab the focus */
  setFocusable(true);
        
  /** as the NetBeans window system manage focus in a strange manner, we should do: */
  addFocusListener(new FocusAdapter {
      override def focusGained(e: FocusEvent) {
        viewContainer.requestFocusInWindow
      }
            
      override def focusLost(e: FocusEvent) {
      }
    })
        

    
    
    
  private def showPopup(e: MouseEvent) {
    if (e.isPopupTrigger()) {
      popup.show(this, e.getX, e.getY)
    }
  }
    
  override def open {
    val size = instanceRefs.size
    val modeSerialNumber = (size - 1) % REALTIME_MODE_NUMBER + 1
    val modeName = MODE + modeSerialNumber
    val mode = WindowManager.getDefault.findMode(modeName)
    mode.dockInto(this)
    super.open
  }
    
  override protected def componentActivated {
    if (stock.isTickerServerSubscribed) {
      stock.subscribeTickerServer
    }
        
    /** active corresponding ticker board */
    for (ref <- instanceRefs; tc = ref.get) {
      if (tc.getStock.equals(stock)) {
        if (!tc.isOpened) {
          tc.open
        }
        tc.requestVisible
      } else {
        tc.close
      }
    }
        
    super.componentActivated
  }
    
  override protected def componentClosed {
    instanceRefs -= ref
    super.componentClosed
  }
    
  override protected def preferredID: String = {
    s_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }
    
  def getStock: Stock = {
    stock
  }
    
  def getViewContainer: RealTimeChartViewContainer = {
    viewContainer
  }
    
  override def getActions: Array[Action] = {
    val actions = super.getActions
    val newActions = new Array[Action](actions.length + 1)
    for (i <- 0 until actions.length) {
      newActions(i) = actions(i)
    }
    newActions(actions.length) = SystemAction.get(classOf[SwitchCandleOhlcAction])
        
    newActions
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    instanceRefs -= ref
    super.finalize
  }
    
    
  /*-
   final TopComponent.Registry registry = TopComponent.getRegistry();
   registry.addPropertyChangeListener(
   new PropertyChangeListener() {
   public void propertyChange(PropertyChangeEvent evt) {
   if (TopComponent.Registry.PROP_OPENED.equals(evt.getPropertyName())) {
   Set openedSet = registry.getOpened();
   if (openedSet != null) {
   for (Iterator iter = openedSet.iterator(); iter.hasNext(); ) {
   TopComponent topComponent = (TopComponent ) iter.next();
   // now see if the topComponent contains Java file
   Node[] nodes =  topComponent.getActivatedNodes();
   if (nodes != null && nodes.length > 0) {
   // you may want to go through all nodes here...I am showing 0th node only
   DataObject dataObject = (DataObject) nodes[0].getLookup().lookup(DataObject.class);
   if (dataObject instanceof HtmlDataObject) {
   FileObject theFile = dataObject.getPrimaryFile();
   OpenJavaClassThread run = new OpenJavaClassThread(theFile);
   RequestProcessor.getDefault().post(run);
   }
   }
   }
   }
   }
   }
   }
   );
   */
    
}

