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
import java.lang.ref.WeakReference;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.chartview.RealTimeBoardPanel
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.securities.Sec
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
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
object RealTimeBoardTopComponent {
  var instanceRefs = List[WeakReference[RealTimeBoardTopComponent]]();

  /** The Mode this component will live in */
  val MODE = "realtimeBoard"


  def findInstance(sec: Sec): RealTimeBoardTopComponent = {
    for (ref <- instanceRefs) {
      if (ref.get().getSec.equals(sec)) {
        return ref.get();
      }
    }

    return null;
  }

  def getInstance(sec: Sec, contents: AnalysisContents): RealTimeBoardTopComponent = {
    var instance = findInstance(sec);
    if (instance == null) {
      instance = new RealTimeBoardTopComponent(sec, contents);
    }

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }



}
class RealTimeBoardTopComponent(sec: Sec, contents: AnalysisContents) extends TopComponent {
  import RealTimeBoardTopComponent._

  private val ref = new WeakReference[RealTimeBoardTopComponent](this)
  instanceRefs ::= ref
    
  private var reallyClosed = false
    
  private val s_id = sec.name + "_TK";
        
  private val boardPanel = new RealTimeBoardPanel(sec, contents);
        
  setLayout(new BorderLayout);
        
  add(boardPanel, BorderLayout.CENTER);
  setName("Realtime - " + sec.uniSymbol);
        
  /** this component should setFocusable(true) to have the ability to grab the focus */
  setFocusable(true);
        
  /** as the NetBeans window system manage focus in a strange manner, we should do: */
  addFocusListener(new FocusAdapter() {
      override def focusGained(e: FocusEvent) {
        getChartViewContainer.requestFocusInWindow
      }
    });
    
  override def open {
    val mode = WindowManager.getDefault().findMode(MODE)
    mode.dockInto(this)
        
    super.open
  }
    
  override protected def componentActivated {
    if (!isOpened) {
      open
    }
        
    /** hidden orthers */
    for (refX <- instanceRefs) {
      if (refX.get != this) {
        refX.get.setReallyClosed(false);
        refX.get.close
      }
    }
        
    super.componentActivated
  }
    
  override protected def componentShowing {
    super.componentShowing
  }
    
  def setReallyClosed(b: Boolean) {
    this.reallyClosed = b
  }
    
  override protected def componentClosed {
    if (reallyClosed) {
      super.componentClosed
    } else {
      val win = WindowManager.getDefault.findTopComponent("RealtimeWatchList");
      if (win.isOpened) {
        /** closing is not allowed */
      } else {
        super.componentClosed
      }
      /**
       * do not remove it from instanceRefs here, so can be called back.
       * remove it by RealtimeWatchListTopComponent
       */
    }
  }
    
  override protected def preferredID: String = {
    s_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER;
  }
    
  def getSec: Sec = {
    sec
  }
    
  def getChartViewContainer: ChartViewContainer = {
    boardPanel.getChartViewContainer
  }
    
  def watch {
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return;
    }
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.addObserver(boardPanel)
    }
  }
    
  def unWatch() {
    val tickerServer = sec.tickerServer
    if (tickerServer == null) {
      return
    }
    
    tickerServer.tickerSnapshotOf(sec.tickerContract.symbol) foreach {tickerSnapshot =>
      tickerSnapshot.deleteObserver(boardPanel)
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    instanceRefs -= ref
    super.finalize
  }
    
}


