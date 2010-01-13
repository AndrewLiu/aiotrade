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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Timer;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.ChartingController;
import org.aiotrade.lib.charting.view.ChartingControllerFactory;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.AIOScrollView;
import org.aiotrade.platform.core.analysis.chartview.RealTimeChartViewContainer;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.modules.netbeans.ui.actions.SwitchCandleOhlcAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.ZoomInAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.ZoomOutAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

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
 *
 * @author Caoyuan Deng
 */
public class RealTimeChartsTopComponent extends TopComponent {
    private static List<WeakReference<RealTimeChartsTopComponent>> instanceRefs =
            new ArrayList<WeakReference<RealTimeChartsTopComponent>>();
    private WeakReference<RealTimeChartsTopComponent> ref;
    
    private final static int SCROLL_SPEED_THROTTLE = 2400; // delay in milli seconds
    
    /** The Mode this component will live in. */
    private static final String MODE = "realtimeCharts";
    
    private String s_id = "RealtimeCharts";
    
    private String symbol;
    
    private List<Sec> secs = new ArrayList<Sec>();
    private List<ChartViewContainer> viewContainers = new ArrayList<ChartViewContainer>();
    
    private MouseAdapter myMouseAdapter;
    
    private JPopupMenu popup;
    
    private JViewport viewPort;
    private AIOScrollView scrollView;
    private Timer scrollTimer;
    private ScrollTimerListener scrollTimerListener;
    
    private boolean reallyClosed;
    
    public RealTimeChartsTopComponent() {
        ref = new WeakReference<RealTimeChartsTopComponent>(this);
        instanceRefs.add(ref);
        
        initComponent();
        
        scrollTimerListener = new ScrollTimerListener();
        scrollTimer = new Timer(SCROLL_SPEED_THROTTLE, scrollTimerListener);
        scrollTimer.setInitialDelay(0);  // default InitialDelay
    }
    
    private void initComponent() {
        setName("Watch List RealTime Charts");
        
        scrollView = new AIOScrollView(this, viewContainers);
        scrollView.setBackground(LookFeel.getCurrent().backgroundColor);
        
        //viewPort = new JViewport();
        //viewPort.setView(scrollView);
        
        setLayout(new BorderLayout());
        add(scrollView, BorderLayout.CENTER);
        
        popup = new JPopupMenu();
        popup.add(SystemAction.get(SwitchCandleOhlcAction.class));
        popup.add(SystemAction.get(SwitchCalendarTradingTimeViewAction.class));
        popup.add(SystemAction.get(ZoomInAction.class));
        popup.add(SystemAction.get(ZoomOutAction.class));
        
        myMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(e);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
        };
        addMouseListener(myMouseAdapter);
        
        /** this component should setFocusable(true) to have the ability to grab the focus */
        setFocusable(true);
    }
    
    public void watch(Sec sec, AnalysisContents contents) {
        if (!secs.contains(sec)) {
            ChartingController controller = ChartingControllerFactory.createInstance(
                    sec.getTickerSer(), contents);
            ChartViewContainer viewContainer = controller.createChartViewContainer(
                    RealTimeChartViewContainer.class, this);
            
            viewContainer.setInteractive(false);
            
            secs.add(sec);
            viewContainers.add(viewContainer);
            
            scrollView.add(viewContainer);
            scrollView.repaint();
        }
        
        scrollTimer.stop();
        scrollTimerListener.startScrollTimerIfNecessary();
    }
    
    public void unWatch(Sec sec) {
        int idx = secs.indexOf(sec);
        if (idx != -1) {
            secs.remove(idx);
            ChartViewContainer viewContainer = viewContainers.get(idx);
            scrollView.remove(viewContainer);
            viewContainers.remove(idx);
        }
    }
    
    public Collection<ChartViewContainer> getViewContainers() {
        return viewContainers;
    }
    
    private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(this, e.getX(), e.getY());
        }
    }
    
    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(MODE);
        mode.dockInto(this);
        
        scrollTimer.stop();
        scrollTimerListener.startScrollTimerIfNecessary();
        
        super.open();
    }
    
    @Override
    protected void componentActivated() {
        super.componentActivated();
    }
    
    public void setReallyClosed(boolean b) {
        this.reallyClosed = b;
    }

    @Override
    protected void componentClosed() {
        scrollTimer.stop();
        
        if (reallyClosed) {
            super.componentClosed();
        } else {
            TopComponent win = WindowManager.getDefault().findTopComponent("RealtimeWatchList");
            if (win.isOpened()) {
                /** closing is not allowed */
            } else {
                super.componentClosed();
            }
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
    
    @Override
    public Action[] getActions() {
        Action[] actions = super.getActions();
        Action[] newActions = new Action[actions.length + 1];
        for (int i = 0; i < actions.length; i++) {
            newActions[i] = actions[i];
        }
        newActions[actions.length] = SystemAction.get(SwitchCandleOhlcAction.class);
        
        return newActions;
    }
    
    @Override
    protected final void finalize() throws Throwable {
        if (myMouseAdapter != null) {
            removeMouseListener(myMouseAdapter);
        }
        instanceRefs.remove(ref);
        super.finalize();
    }
    
    public static List<WeakReference<RealTimeChartsTopComponent>> getInstanceRefs() {
        return instanceRefs;
    }
    
    public static RealTimeChartsTopComponent getInstance() {
        RealTimeChartsTopComponent instance = null;
        
        if (instanceRefs.size() == 0) {
            instance = new RealTimeChartsTopComponent();
        } else {
            instance = instanceRefs.get(0).get();
        }
        
        if (!instance.isOpened()) {
            instance.open();
        }
        
        return instance;
    }
    
    
    /**
     * Listener for timer events.
     */
    private class ScrollTimerListener implements ActionListener {
        
        public void startScrollTimerIfNecessary() {
            if (viewContainers.size() <= 0 || scrollTimer.isRunning()) {
                return;
            }
            
            scrollTimer.start();
        }
        
        public void actionPerformed(ActionEvent e) {
            /** mouse is in this TopComponent? if yes, do nothing, else scroll */
            if (RealTimeChartsTopComponent.this.getMousePosition() == null) {
                scrollView.setBackground(LookFeel.getCurrent().backgroundColor);
                scrollView.scrollByPicture(1);
                //scrollView.scrollByPixel(3);
            }
        }
    }
    
    
}


