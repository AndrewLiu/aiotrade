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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.sec.TickerSerProvider;
import org.aiotrade.platform.core.sec.TickerSnapshot;
import org.aiotrade.platform.modules.ui.panel.RealtimeWatchListPanel;
import org.aiotrade.platform.modules.netbeans.ui.actions.StartSelectedWatchAction;
import org.aiotrade.platform.modules.netbeans.ui.actions.StopSelectedWatchAction;
import org.aiotrade.platform.modules.netbeans.ui.explorer.SymbolNode.SymbolStartWatchAction;
import org.aiotrade.platform.modules.netbeans.ui.explorer.SymbolNode.SymbolStopWatchAction;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
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
public class RealTimeWatchListTopComponent extends TopComponent {
    private static List<WeakReference<RealTimeWatchListTopComponent>> instanceRefs =
            new ArrayList<WeakReference<RealTimeWatchListTopComponent>>();
    private WeakReference<RealTimeWatchListTopComponent> ref;
    
    /** The Mode this component will live in. */
    private static final String MODE = "realtimeWatchList";
    
    private String s_id = "RealtimeWatchList";
    
    private RealtimeWatchListPanel rtWatchListPanel;
    
    private boolean updateServerRegistered = false;
    
    private static Set<Sec> watchingSecSet = new HashSet<Sec>();
    
    private Map<String, Node> symbolMapNode = new HashMap<String, Node>();
    
    private JPopupMenu popup;
    
    public RealTimeWatchListTopComponent() {
        ref = new WeakReference<RealTimeWatchListTopComponent>(this);
        instanceRefs.add(ref);
        init();
    }
    
    private void init() {
        initComponent();
    }
    
    
    private void initComponent() {
        rtWatchListPanel = new RealtimeWatchListPanel();
        
        setLayout(new BorderLayout());
        
        add(rtWatchListPanel, BorderLayout.CENTER);
        setName("Watch List");
        
        popup = new JPopupMenu();
        popup.add(SystemAction.get(StartSelectedWatchAction.class));
        popup.add(SystemAction.get(StopSelectedWatchAction.class));
        
        JTable watchListTable = rtWatchListPanel.getWatchListTable();
        
        watchListTable.addMouseListener(new WatchListTableMouseListener(watchListTable, this));
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
        super.open();
    }
    
    @Override
    protected void componentShowing() {
        super.componentShowing();
    }
    
    @Override
    protected void componentClosed() {
        stopAllWatch();
        
        for (WeakReference<RealTimeChartsTopComponent> refX : RealTimeChartsTopComponent.getInstanceRefs()) {
            refX.get().setReallyClosed(true);
            refX.get().close();
        }
        RealTimeChartsTopComponent.getInstanceRefs().clear();
        
        for (WeakReference<RealTimeBoardTopComponent> refX : RealTimeBoardTopComponent.getInstanceRefs()) {
            refX.get().setReallyClosed(true);
            refX.get().close();
        }
        RealTimeBoardTopComponent.getInstanceRefs().clear();
        
        instanceRefs.remove(ref);
        super.componentClosed();
    }
    
    public void watch(Sec sec, Node node) {
        rtWatchListPanel.watch(sec.getUniSymbol());
        symbolMapNode.put(sec.getUniSymbol(), node);
        watchingSecSet.add(sec);
        
        TickerServer tickerServer =  sec.getTickerServer();
        if (tickerServer == null) {
            return;
        }
        TickerSnapshot tickerSnapshot = tickerServer.getTickerSnapshot(sec.getTickerContract().getSymbol());
        if (tickerSnapshot != null) {
            tickerSnapshot.addObserver(rtWatchListPanel);
        }
    }
    
    public void unWatch(TickerSerProvider sec) {
        String uniSymbol = sec.getUniSymbol();
        rtWatchListPanel.unWatch(uniSymbol);
        
        TickerServer tickerServer =  sec.getTickerServer();
        if (tickerServer == null) {
            return;
        }
        TickerSnapshot tickerSnapshot = tickerServer.getTickerSnapshot(sec.getTickerContract().getSymbol());
        if (tickerSnapshot != null) {
            tickerSnapshot.deleteObserver(rtWatchListPanel);
        }
        
        /**
         * !NOTICE
         * don't remove from tickerNodeMap, because you may need to restart it
         *    tickerNodeMap.remove(tickeringSignSeriesProvider.getSymbol());
         */
    }
    
    public List<Node> getSelectedSymbolNodes() {
        List<Node> selectedNodes = new ArrayList<Node>();
        for (int row : rtWatchListPanel.getWatchListTable().getSelectedRows()) {
            String symbol = rtWatchListPanel.getSymbolAtRow(row);
            if (symbol != null) {
                Node node = symbolMapNode.get(symbol);
                if (node != null) {
                    selectedNodes.add(node);
                }
            }
        }
        return selectedNodes;
    }
    
    private List<Node> getAllSymbolNodes() {
        List<Node> nodes = new ArrayList<Node>();
        for (int row = 0; row < rtWatchListPanel.getWatchListTable().getRowCount(); row++) {
            String symbol = rtWatchListPanel.getSymbolAtRow(row);
            if (symbol != null) {
                Node node = symbolMapNode.get(symbol);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
        return nodes;
        
    }
    
    private void stopAllWatch() {
        for (Node node : getAllSymbolNodes()) {
            SymbolStopWatchAction action = node.getLookup().lookup(SymbolStopWatchAction.class);
            if (action != null) {
                action.execute();
            }
        }
        
        for (Sec sec : watchingSecSet) {
            TickerServer tickerServer = sec.getTickerServer();
            if (tickerServer != null && tickerServer.isInUpdating()) {
                tickerServer.stopUpdateServer();
            }
        }
        
        /** should clear tickerWatchListPanel */
        rtWatchListPanel.clearAllWatch();
        watchingSecSet.clear();
        symbolMapNode.clear();
    }
    
    private class WatchListTableMouseListener implements MouseListener {
        private JTable table;
        private JComponent receiver;
        
        public WatchListTableMouseListener(JTable table, JComponent receiver) {
            this.table = table;
            this.receiver = receiver;
        }
        
        private int getRowAtY(MouseEvent e) {
            TableColumnModel columnModel = table.getColumnModel();
            int col = columnModel.getColumnIndexAtX(e.getX());
            int row = e.getY() / table.getRowHeight();
            
            return row;
        }
        
        public void mouseClicked(MouseEvent e) {
            showPopup(e);
        }
        
        public void mousePressed(MouseEvent e) {
            showPopup(e);
            
            /** when double click on a row, try to active this stock's tickering chart view */
            if (e.getClickCount() == 2) {
                String symbol = rtWatchListPanel.getSymbolAtRow(getRowAtY(e));
                if (symbol == null) {
                    return;
                }
                
                Node node = symbolMapNode.get(symbol);
                if (node != null) {
                    SymbolStartWatchAction watchAction = node.getLookup().lookup(SymbolStartWatchAction.class);
                    if (watchAction != null) {
                        watchAction.execute();
                    }
                }
            }
        }
        
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        
        public void mouseEntered(MouseEvent e) {
        }
        
        public void mouseExited(MouseEvent e) {
        }
    }
    
    
    protected String preferredID() {
        return s_id;
    }
    
    public int getPersistenceType() {
        return this.PERSISTENCE_NEVER;
    }
    
    @Override
    protected final void finalize() throws Throwable {
        instanceRefs.remove(ref);
        super.finalize();
    }
    
    public static List<WeakReference<RealTimeWatchListTopComponent>> getInstanceRefs() {
        return instanceRefs;
    }
    
    public static RealTimeWatchListTopComponent getInstance() {
        RealTimeWatchListTopComponent instance = instanceRefs.size() == 0 ?
            new RealTimeWatchListTopComponent() : instanceRefs.get(0).get();
        
        if (!instance.isOpened()) {
            instance.open();
        }
        
        return instance;
    }
    
}



