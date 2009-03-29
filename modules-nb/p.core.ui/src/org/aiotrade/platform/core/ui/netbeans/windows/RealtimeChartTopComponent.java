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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.ChartingControllerFactory;
import org.aiotrade.platform.core.analysis.chartview.RealtimeChartViewContainer;
import org.aiotrade.platform.core.sec.Stock;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchCandleOhlcAction;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.platform.core.ui.netbeans.actions.ZoomInAction;
import org.aiotrade.platform.core.ui.netbeans.actions.ZoomOutAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.Mode;
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
public class RealtimeChartTopComponent extends TopComponent {
    private static List<WeakReference<RealtimeChartTopComponent>> instanceRefs =
            new ArrayList<WeakReference<RealtimeChartTopComponent>>();
    private WeakReference<RealtimeChartTopComponent> ref;
    
    /** The Mode this component will live in. */
    private static final String MODE = "realtime_";
    private static final int REALTIME_MODE_NUMBER = 3;
    
    private String s_id = "";
    
    private String symbol;
    
    private RealtimeChartViewContainer viewContainer;
    
    private Stock stock;
    private AnalysisContents contents;
    
    private JPopupMenu popup;
    
    public RealtimeChartTopComponent(Stock stock, AnalysisContents contents) {
        this.stock = stock;
        this.contents = contents;
        
        ref = new WeakReference<RealtimeChartTopComponent>(this);
        instanceRefs.add(ref);
        init();
    }
    
    private void init() {
        symbol = stock.getUniSymbol();
        this.s_id = stock.getName() + "_RT";
        
        //        if (!stock.isSeriesLoaded()) {
        //            stock.loadSeries();
        //        }
        
        ChartingController controller = ChartingControllerFactory.createInstance(
                stock.getTickerSer(), contents);
        viewContainer = controller.createChartViewContainer(
                RealtimeChartViewContainer.class, this);
        
        initComponent();
    }
    
    
    private void initComponent() {
        setLayout(new BorderLayout());
        
        add(viewContainer, BorderLayout.CENTER);
        setName(stock.getName() + "_RT");
        
        popup = new JPopupMenu();
        popup.add(SystemAction.get(SwitchCandleOhlcAction.class));
        popup.add(SystemAction.get(SwitchCalendarTradingTimeViewAction.class));
        popup.add(SystemAction.get(ZoomInAction.class));
        popup.add(SystemAction.get(ZoomOutAction.class));
        
        addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                showPopup(e);
            }
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
            public void mouseEntered(MouseEvent e) {
            }
            public void mouseExited(MouseEvent e) {
            }
        });
        
        /** this component should setFocusable(true) to have the ability to grab the focus */
        setFocusable(true);
        
        /** as the NetBeans window system manage focus in a strange manner, we should do: */
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                viewContainer.requestFocusInWindow();
            }
            
            public void focusLost(FocusEvent e) {
            }
        });
        
    }
    
    
    
    private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(this, e.getX(), e.getY());
        }
    }
    
    public void open() {
        int size = getInstanceRefs().size();
        int modeSerialNumber = (size - 1) % REALTIME_MODE_NUMBER + 1;
        String modeName = MODE + modeSerialNumber;
        Mode mode = WindowManager.getDefault().findMode(modeName);
        mode.dockInto(this);
        super.open();
    }
    
    protected void componentActivated() {
        if (stock.isTickerServerSubscribed()) {
            stock.subscribeTickerServer();
        }
        
        /** active corresponding ticker board */
        for (WeakReference<RealtimeBoardTopComponent> ref : RealtimeBoardTopComponent.getInstanceRefs()) {
            if (ref.get().getSec().equals(stock)) {
                if (!ref.get().isOpened()) {
                    ref.get().open();
                }
                ref.get().requestVisible();
            } else {
                ref.get().close();
            }
        }
        
        super.componentActivated();
    }
    
    protected void componentClosed() {
        instanceRefs.remove(ref);
        super.componentClosed();
    }
    
    protected String preferredID() {
        return s_id;
    }
    
    public int getPersistenceType() {
        return this.PERSISTENCE_NEVER;
    }
    
    public Stock getStock() {
        return (Stock)stock;
    }
    
    public RealtimeChartViewContainer getViewContainer() {
        return viewContainer;
    }
    
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
        instanceRefs.remove(ref);
        super.finalize();
    }
    
    public static List<WeakReference<RealtimeChartTopComponent>> getInstanceRefs() {
        return instanceRefs;
    }
    
    public static RealtimeChartTopComponent getInstance(Stock stock, AnalysisContents contents) {
        RealtimeChartTopComponent instance = null;
        
        for (WeakReference<RealtimeChartTopComponent> ref : RealtimeChartTopComponent.getInstanceRefs()) {
            if (ref.get().getStock().equals(stock)) {
                instance = ref.get();
            }
        }
        
        if (instance == null) {
            instance = new RealtimeChartTopComponent(stock, contents);
        }
        
        if (!instance.isOpened()) {
            instance.open();
        }
        
        return instance;
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

