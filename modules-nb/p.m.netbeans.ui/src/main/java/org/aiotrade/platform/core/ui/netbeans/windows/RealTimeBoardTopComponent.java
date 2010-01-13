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
package org.aiotrade.platform.core.ui.netbeans.windows;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.analysis.chartview.RealTimeBoardPanel;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.sec.TickerSnapshot;
import org.openide.windows.Mode;
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
public class RealTimeBoardTopComponent extends TopComponent {
    private static List<WeakReference<RealTimeBoardTopComponent>> instanceRefs =
            new ArrayList<WeakReference<RealTimeBoardTopComponent>>();
    private WeakReference<RealTimeBoardTopComponent> ref;
    
    /** The Mode this component will live in */
    public static final String MODE = "realtimeBoard";
    
    private String s_id = "RealtimeBoard";
    
    private RealTimeBoardPanel boardPanel;
    
    private Sec sec;
    private AnalysisContents contents;
    
    private boolean reallyClosed;
    
    public RealTimeBoardTopComponent(Sec sec, AnalysisContents contents) {
        this.sec = sec;
        this.contents = contents;
        
        ref = new WeakReference<RealTimeBoardTopComponent>(this);
        instanceRefs.add(ref);
        init();
    }
    
    private void init() {
        this.s_id = sec.getName() + "_TK";
        
        initComponent();
    }
    
    private void initComponent() {
        boardPanel = new RealTimeBoardPanel(sec, contents);
        
        setLayout(new BorderLayout());
        
        add(boardPanel, BorderLayout.CENTER);
        setName("Realtime - " + sec.getUniSymbol());
        
        /** this component should setFocusable(true) to have the ability to grab the focus */
        setFocusable(true);
        
        /** as the NetBeans window system manage focus in a strange manner, we should do: */
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                getChartViewContainer().requestFocusInWindow();
            }
        });
    }
    
    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(MODE);
        mode.dockInto(this);
        
        super.open();
    }
    
    @Override
    protected void componentActivated() {
        if (!isOpened()) {
            open();
        }
        
        /** hidden orthers */
        for (WeakReference<RealTimeBoardTopComponent> refX : getInstanceRefs()) {
            if (refX.get() != this) {
                refX.get().setReallyClosed(false);
                refX.get().close();
            }
        }
        
        super.componentActivated();
    }
    
    @Override
    protected void componentShowing() {
        super.componentShowing();
    }
    
    public void setReallyClosed(boolean b) {
        this.reallyClosed = b;
    }
    
    @Override
    protected void componentClosed() {
        if (reallyClosed) {
            super.componentClosed();
        } else {
            TopComponent win = WindowManager.getDefault().findTopComponent("RealtimeWatchList");
            if (win.isOpened()) {
                /** closing is not allowed */
            } else {
                super.componentClosed();
            }
            /**
             * do not remove it from instanceRefs here, so can be called back.
             * remove it by RealtimeWatchListTopComponent
             */
        }
    }
    
    @Override
    protected String preferredID() {
        return s_id;
    }
    
    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
    
    public Sec getSec() {
        return (Sec)sec;
    }
    
    public ChartViewContainer getChartViewContainer() {
        return boardPanel.getChartViewContainer();
    }
    
    public void watch() {
        TickerServer tickerServer =  sec.getTickerServer();
        if (tickerServer == null) {
            return;
        }
        TickerSnapshot tickerSnapshot = tickerServer.getTickerSnapshot(sec.getTickerContract().getSymbol());
        if (tickerSnapshot != null) {
            tickerSnapshot.addObserver(boardPanel);
        }
    }
    
    public void unWatch() {
        TickerServer tickerServer =  sec.getTickerServer();
        if (tickerServer == null) {
            return;
        }
        TickerSnapshot tickerSnapshot = tickerServer.getTickerSnapshot(sec.getTickerContract().getSymbol());
        if (tickerSnapshot != null) {
            tickerSnapshot.deleteObserver(boardPanel);
        }
    }
    
    @Override
    protected final void finalize() throws Throwable {
        instanceRefs.remove(ref);
        super.finalize();
    }
    
    public static List<WeakReference<RealTimeBoardTopComponent>> getInstanceRefs() {
        return instanceRefs;
    }
    
    public static RealTimeBoardTopComponent findInstance(Sec sec) {
        for (WeakReference<RealTimeBoardTopComponent> ref : RealTimeBoardTopComponent.getInstanceRefs()) {
            if (ref.get().getSec().equals(sec)) {
                return ref.get();
            }
        }
        
        return null;
    }
    
    public static RealTimeBoardTopComponent getInstance(Sec sec, AnalysisContents contents) {
        RealTimeBoardTopComponent instance = findInstance(sec);
        if (instance == null) {
            instance = new RealTimeBoardTopComponent(sec, contents);
        }
        
        if (!instance.isOpened()) {
            instance.open();
        }
        
        return instance;
    }
    
}


