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
package org.aiotrade.platform.core.ui.netbeans.explorer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.util.swing.action.GeneralAction;
import org.aiotrade.util.swing.action.SaveAction;
import org.aiotrade.util.swing.action.ViewAction;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.platform.core.analysis.chartview.AnalysisQuoteChartView;
import org.aiotrade.math.timeseries.SerChangeListener;
import org.aiotrade.platform.core.netbeans.NetBeansPersistenceManager;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.sec.Stock;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.platform.core.analysis.ContentsParseHandler;
import org.aiotrade.platform.core.analysis.indicator.QuoteCompareIndicator;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.platform.core.netbeans.GroupDescriptor;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.ui.netbeans.windows.AnalysisChartTopComponent;
import org.aiotrade.platform.core.ui.netbeans.actions.AddSymbolAction;
import org.aiotrade.platform.core.ui.dialog.ImportSymbolDialog;
import org.aiotrade.platform.core.ui.netbeans.windows.RealtimeChartsTopComponent;
import org.aiotrade.platform.core.ui.netbeans.windows.RealtimeBoardTopComponent;
import org.aiotrade.platform.core.ui.netbeans.windows.RealtimeWatchListTopComponent;
import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.loaders.XMLDataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.FilterNode;
import org.openide.nodes.FilterNode.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.WindowManager;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * SymbolNode is a representation for sec serialization file "xxx.ser"  or
 * folder contains these files.
 *
 *  The tree view of Symbol and others
 *  + Symbols (config/Symbols)
 *    +- sunw (sunw.ser)
 *       +- Indicators (DescriptorGroupNode)
 *       |  +- MACD (DescriptorNode)
 *       |  |   +-opt1
 *       |  |   +-opt2
 *       |  +- ROC
 *       |     +-opt1
 *       |     +-opt2
 *       +- Drawings (DescriptorGroupNode)
 *          +- layer1
 *          |  +- line
 *          |  +- parallel
 *          |  +- gann period
 *          +- layer2
 *
 *
 *
 *
 * @author Caoyuan Deng
 */
public class SymbolNode extends FilterNode {
    private final static Image DEFAUTL_SOURCE_ICON = Utilities.loadImage("org/aiotrade/platform/core/ui/netbeans/resources/symbol.gif");
    /**
     * Declaring the children of the root sec node
     *
     *
     * @param symbolFolderNode: the folder or file('stockname.ser') which delegated to this node
     */
    public SymbolNode(Node symbolFolderNode) throws DataObjectNotFoundException, IntrospectionException {
        this(symbolFolderNode, new InstanceContent());
    }
    
    private SymbolNode(Node symbolFolderNode, InstanceContent content) throws IntrospectionException {
        /** Create proxy of node with a different set of children. */
        super(symbolFolderNode, new SymbolFolderChildren(symbolFolderNode), new AbstractLookup(content));
        
        /* add the node to our own lookup */
        content.add(this);
        
        /** add delegate's all lookup contents */
        Lookup.Result<Object> result = symbolFolderNode.getLookup().lookup(new Lookup.Template<Object>(Object.class));
        for (Object o : result.allInstances()) {
            content.add(o);
        }
        
        /* add additional items to the lookup */
        content.add(SystemAction.get(AddSymbolAction.class));
        content.add(new SymbolStartWatchAction(this));
        content.add(new SymbolStopWatchAction(this));
        content.add(new SymbolRefreshDataAction(this));
        content.add(new SymbolReimportDataAction(this));
        content.add(new SymbolViewAction(this));
    }
    
    /** Declaring the actions that can be applied to this node */
    public Action[] getActions(boolean popup) {
        DataFolder df = getLookup().lookup(DataFolder.class);
        return new Action[] {
            getLookup().lookup(AddSymbolAction.class),
            new AddFolderAction(df),
            null,
            getLookup().lookup(SymbolViewAction.class),
            null,
            getLookup().lookup(SymbolStartWatchAction.class),
            getLookup().lookup(SymbolStopWatchAction.class),
            null,
            getLookup().lookup(SymbolRefreshDataAction.class),
            getLookup().lookup(SymbolReimportDataAction.class),
            null,
            SystemAction.get(DeleteAction.class),
        };
    }
    
    /**
     * The root node of SymbolNode
     *  It will be 'Symbols' folder in default file system, usually the 'config' dir in userdir
     *  Physical folder "Symbols" is defined in layer.xml
     */
    public static class RootSymbolNode extends SymbolNode {
        public RootSymbolNode() throws DataObjectNotFoundException, IntrospectionException {
            super(DataObject.find(
                    Repository.getDefault().getDefaultFileSystem().getRoot()
                    .getFileObject("Symbols")).getNodeDelegate());
        }
        
        public String getDisplayName() {
            return NbBundle.getMessage(SymbolNode.class, "SN_title");
        }
    }
    
    /** The child of the folder node, it may be a folder or Symbol ser file */
    private static class SymbolFolderChildren extends FilterNode.Children {
        Node oneSymbolNode;
        
        SymbolFolderChildren(Node symbolFolderNode) {
            super(symbolFolderNode);
        }
        
        protected Node[] createNodes(Node key) {
            Node node = key;
            
            try {
                /** is a folder? if true, creat a folder node */
                if (node.getLookup().lookup(DataFolder.class) != null) {
                    return new Node[] { new SymbolNode(node) };
                }
                /**
                 * else, deserilize a contents instance from the sec xml file,
                 * and create a sec node for it
                 */
                else {
                    AnalysisContents contents = readContents(node);
                    if (contents != null) {
                        /**
                         * check if has existed in application context, if true,
                         * use the existed one
                         */
                        AnalysisContents existedOne = NetBeansPersistenceManager.lookupContents(contents.getUniSymbol());
                        if (existedOne != null) {
                            contents = existedOne;
                        }
                        
                        oneSymbolNode = new OneSymbolNode(node, contents);
                        FileObject fileObject = oneSymbolNode.getLookup().lookup(DataObject.class).getPrimaryFile();
                        
                        Object newAttr = fileObject.getAttribute("new");
                        if ( newAttr != null && (Boolean)newAttr == true) {
                            fileObject.setAttribute("new", false);
                            
                            /** open view for new added sec */
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    oneSymbolNode.getLookup().lookup(ViewAction.class).execute();
                                }
                            });
                        }
                        
                        return new Node[] { oneSymbolNode };
                        
                    } else {
                        // best effort
                        return new Node[] { new FilterNode(node) };
                        
                    }
                    
                }
            } catch (IOException ioe) {
                ErrorManager.getDefault().notify(ioe);
            } catch (IntrospectionException exc) {
                ErrorManager.getDefault().notify(exc);
            }
            
            // Some other type of Node (gotta do something)
            return new Node[] { new FilterNode(node) };
        }
        
    }
    
    /** Getting the Symbol node and wrapping it in a FilterNode */
    public static class OneSymbolNode extends FilterNode {
        
        /* As the lookup needs to be constucted before Node's constructor is called,
         * it might not be obvious how to add Node or other objects into it without
         * type casting. Here is the recommended suggestion that uses public/private
         * pair of constructors:
         */
        private OneSymbolNode(Node symbolFileNode, AnalysisContents contents) throws IOException, IntrospectionException {
            this(symbolFileNode, contents, new InstanceContent());
        }
        
        private OneSymbolNode(Node symbolFileNode, AnalysisContents contents, InstanceContent content) {
            super(symbolFileNode, new SymbolChildren(contents), new AbstractLookup(content));
            
            NetBeansPersistenceManager.putNode(contents, this);
            
            /* add the node to our own lookup */
            content.add(this);
            
            /* add additional items to the lookup */
            content.add(contents);
            content.add(new SymbolViewAction(this));
            content.add(new SymbolReimportDataAction(this));
            content.add(new SymbolRefreshDataAction(this));
            content.add(new SymbolSetDataSourceAction(this));
            content.add(new SymbolStartWatchAction(this));
            content.add(new SymbolStopWatchAction(this));
            content.add(new SymbolCompareToAction(this));
            content.add(new SymbolClearDataAction(this));
            
            /** add delegate's all lookup contents */
            Lookup.Result<Object> result = symbolFileNode.getLookup().lookup(new Lookup.Template<Object>(Object.class));
            for (Object o : result.allInstances()) {
                content.add(o);
            }
        }
        
        public String getDisplayName() {
            AnalysisContents contents = getLookup().lookup(AnalysisContents.class);
            return  contents.getUniSymbol();
        }
        
        public Image getIcon(int type) {
            Image icon = null;
            
            AnalysisContents contents = getLookup().lookup(AnalysisContents.class);
            QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
            if (quoteContract != null) {
                icon = quoteContract.getIcon();
            }
            
            return icon != null ? icon : DEFAUTL_SOURCE_ICON;
        }
        
        public Image getOpenedIcon(int type) {
            return getIcon(0);
        }
        
        public Action[] getActions(boolean context) {
            return new Action[] {
                getLookup().lookup(SymbolViewAction.class),
                getLookup().lookup(SymbolRefreshDataAction.class),
                getLookup().lookup(SymbolReimportDataAction.class),
                null,
                getLookup().lookup(SymbolStartWatchAction.class),
                getLookup().lookup(SymbolStopWatchAction.class),
                null,
                getLookup().lookup(SymbolCompareToAction.class),
                null,
                getLookup().lookup(SymbolSetDataSourceAction.class),
                null,
                getLookup().lookup(SymbolClearDataAction.class),
                SystemAction.get(DeleteAction.class),
            };
        }
        
        /**
         * The getPreferredAction() simply returns the action that should be
         * run if the user double-clicks this node
         */
        public Action getPreferredAction() {
            return getActions(true)[0];
        }
        
        protected NodeListener createNodeListener() {
            final NodeListener delegate = super.createNodeListener();
            NodeListener newListener = new NodeListener() {
                public void childrenAdded(NodeMemberEvent nodeMemberEvent) {
                    delegate.childrenAdded(nodeMemberEvent);
                }
                
                public void childrenRemoved(NodeMemberEvent nodeMemberEvent) {
                    delegate.childrenRemoved(nodeMemberEvent);
                }
                
                public void childrenReordered(NodeReorderEvent nodeReorderEvent) {
                    delegate.childrenReordered(nodeReorderEvent);
                }
                
                public void nodeDestroyed(NodeEvent nodeEvent) {
                    /**
                     * We should check if this is a delete call, and clear data in db
                     * only when true, since it will also be called when you move a
                     * node from a folder to another.
                     * The checking is simplely like the following code:
                     * if returns null, means another copy-pasted node has been created,
                     * and owned the descriptors now, so returns null, in this case,
                     * it's a moving call. Otherwise, if returns no null, we are sure
                     * this is a real delete call other than moving.
                     * @NOTICE
                     * Here we should find via OneSymbolNode.this instead of nodeEvent.getNode(),
                     * which may return the delegated node.
                     */
                    if (NetBeansPersistenceManager.getOccupiedContents(OneSymbolNode.this) != null) {
                        getLookup().lookup(SymbolClearDataAction.class).perform(false);
                    }
                    
                    NetBeansPersistenceManager.removeNode(nodeEvent.getNode());
                    delegate.nodeDestroyed(nodeEvent);
                }
                
                public void propertyChange(PropertyChangeEvent evt) {
                    delegate.propertyChange(evt);
                }
            };
            return newListener;
        }
        
        
    }
    
    /**
     * The children wrap class
     * ------------------------------------------------------------------------
     *
     * Defining the all children of a Symbol node
     * They will be representation for descriptorGroups in this case. it's simply
     * a wrap class of DescriptorGroupNode with addNotify() and createNodes()
     * implemented
     *
     * Typical usage of Children.Keys:
     *
     *  1. Subclass.
     *  2. Decide what type your key should be.
     *  3. Implement createNodes(java.lang.Object) to create some nodes (usually exactly one) per key.
     *  4. Override Children.addNotify() to construct a set of keys and set it using setKeys(Collection). The collection may be ordered.
     *  5. Override Children.removeNotify() to just call setKeys on Collections.EMPTY_SET.
     *  6. When your model changes, call setKeys with the new set of keys. Children.Keys will be smart and calculate exactly what it needs to do effficiently.
     *  7. (Optional) if your notion of what the node for a given key changes (but the key stays the same), you can call refreshKey(java.lang.Object). Usually this is not necessary.
     */
    private static class SymbolChildren extends Children.Keys<GroupDescriptor<AnalysisDescriptor>> {
        
        private AnalysisContents contents;
        
        public SymbolChildren(AnalysisContents contents) {
            this.contents = contents;
        }
        
        /**
         * Called when children are first asked for nodes. Typical implementations at this time
         * calculate their node list (or keys for Children.Keys etc.).
         *
         * !Notice: call to getNodes() inside of this method will return an empty array of nodes.
         *
         * Since setKeys(childrenKeys) will copy the elements of childrenKeys, it's safe to
         * use a repeatly used bufChildrenKeys here.
         * And, to sort them in letter order, we can use a SortedSet to copy from collection.(TODO)
         */
        private Set<GroupDescriptor<AnalysisDescriptor>> bufChildrenKeys = new HashSet<GroupDescriptor<AnalysisDescriptor>>();
        @SuppressWarnings("unchecked")
        protected void addNotify() {
            Collection<GroupDescriptor> groups = PersistenceManager.getDefault().lookupAllRegisteredServices(GroupDescriptor.class, "DescriptorGroups");
            
            bufChildrenKeys.clear();
            /** each symbol should create new NodeInfo instances that belong to itself */
            for (GroupDescriptor nodeInfo : groups) {
                bufChildrenKeys.add((GroupDescriptor<AnalysisDescriptor>)nodeInfo.clone());
            }
            setKeys(bufChildrenKeys);
        }
        
        public Node[] createNodes(GroupDescriptor<AnalysisDescriptor> key) {
            try {
                return new Node[] { new GroupNode(key, contents) };
            } catch (final IntrospectionException ex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                /** Should never happen - no reason for it to fail above */
                return new Node[] { new AbstractNode(Children.LEAF) {
                    public String getHtmlDisplayName() {
                        return "<font color='red'>" + ex.getMessage() + "</font>";
                    }
                }};
            }
        }
        
    }
    
    private static class SymbolViewAction extends ViewAction {
        
        private Node node;
        
        public SymbolViewAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "View");
        }
        
        public void execute() {
            /** is this a folder ? if true, go recursively */
            if (node.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : node.getChildren().getNodes()) {
                    child.getLookup().lookup(SymbolViewAction.class).execute();
                }
                return;
            }
            
            /** otherwise, it's an OneSymbolNode, do real things */
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
            
            Sec sec = (Sec)contents.getSerProvider();
            
            boolean mayNeedsReload = false;
            if (sec == null) {
                sec = new Stock(contents.getUniSymbol(), quoteContract);
                contents.setSerProvider(sec);
            } else {
                mayNeedsReload = true;
            }
            
            AnalysisChartTopComponent analysisTc = AnalysisChartTopComponent.lookupTopComponent(sec.getUniSymbol());
            /**
             * !NOTICE
             * close a TopComponent doen's mean this TopComponent is null, it still
             * exsit, just invsible
             */
            if (analysisTc == null) {
                /** if TopComponent of this stock has been shown before, should reload quote data */
                if (mayNeedsReload) {
                    sec.clearSer(quoteContract.getFreq());
                }
                /** here should be the only place to new AnalysisChartTopComponent instance */
                analysisTc = new AnalysisChartTopComponent(sec, contents);
            }
            
            if (!sec.isSerLoaded(quoteContract.getFreq())) {
                sec.loadSer(quoteContract.getFreq());
            }
            
            if (!analysisTc.isOpened()) {
                analysisTc.open();
            }
            
            analysisTc.requestActive();
        }
        
    }
    
    public static class SymbolStartWatchAction extends GeneralAction {
        
        private Node node;
        
        public SymbolStartWatchAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Start Watching");
            putValue(Action.SMALL_ICON, "org/aiotrade/platform/core/ui/netbeans/resources/startWatch.gif");
        }
        
        public void execute() {
            /** is this a folder ? if true, go recursively */
            if (node.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : node.getChildren().getNodes()) {
                    child.getLookup().lookup(SymbolStartWatchAction.class).execute();
                }
                return;
            }
            
            /** otherwise, it's an OneSymbolNode, do real things */
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            
            Sec sec = (Sec)contents.getSerProvider();
            if (sec == null) {
                QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
                sec = new Stock(contents.getUniSymbol(), quoteContract);
                contents.setSerProvider(sec);
            }
            
            sec.subscribeTickerServer();
            
            RealtimeWatchListTopComponent rtWatchListWin = RealtimeWatchListTopComponent.getInstance();
            rtWatchListWin.requestActive();
            rtWatchListWin.watch(sec, node);
            
            RealtimeChartsTopComponent rtChartsWin = RealtimeChartsTopComponent.getInstance();
            rtChartsWin.requestActive();
            rtChartsWin.watch(sec, contents);
            
            RealtimeBoardTopComponent rtBoardWin = RealtimeBoardTopComponent.getInstance(sec, contents);
            rtBoardWin.watch();
            rtBoardWin.requestActive();
            
            node.getLookup().lookup(SymbolStopWatchAction.class).setEnabled(true);
            this.setEnabled(false);
        }
        
    }
    
    public static class SymbolStopWatchAction extends GeneralAction {
        
        private Node node;
        
        public SymbolStopWatchAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Stop Watching");
            putValue(Action.SMALL_ICON, "org/aiotrade/platform/core/ui/netbeans/resources/stopWatch.gif");
            if (node.getLookup().lookup(DataFolder.class) != null) {
                this.setEnabled(true);
            } else {
                this.setEnabled(false);
            }
        }
        
        public void execute() {
            /** is this a folder ? if true, go recursively */
            if (node.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : node.getChildren().getNodes()) {
                    child.getLookup().lookup(SymbolStopWatchAction.class).execute();
                }
                return;
            }
            
            /** otherwise, it's an OneSymbolNode, do real things */
            
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            
            Sec sec = (Sec)contents.getSerProvider();
            if (sec == null) {
                return;
            }
            
            if (RealtimeWatchListTopComponent.getInstanceRefs().size() > 0) {
                RealtimeWatchListTopComponent.getInstanceRefs().get(0).get().unWatch(sec);
            }
            
            if (RealtimeChartsTopComponent.getInstanceRefs().size() > 0) {
                RealtimeChartsTopComponent.getInstanceRefs().get(0).get().unWatch(sec);
            }
            
            RealtimeBoardTopComponent rtBoardWin = RealtimeBoardTopComponent.findInstance(sec);
            if (rtBoardWin != null) {
                rtBoardWin.unWatch();
            }
            
            sec.unSubscribeTickerServer();
            
            node.getLookup().lookup(SymbolStartWatchAction.class).setEnabled(true);
            this.setEnabled(false);
        }
        
    }
    
    /**
     * We We shouldn't implement deleting data in db in NodeListener#nodeDestroyed(NodeEvent),
     * since  it will be called also when you move a node from a folder to another
     * folder. So we need a standalone action here.
     *
     * @TODO
     */
    public static class SymbolClearDataAction extends GeneralAction {
        private final static String CLEAR = "Clear data in database";
        private final OneSymbolNode node;
        
        SymbolClearDataAction(OneSymbolNode node) {
            this.node = node;
            putValue(Action.NAME, CLEAR);
        }
        
        public void perform(boolean shouldConfirm) {
            /**
             * don't get descriptors from getLookup().lookup(..), becuase
             * if node destroy is invoked by parent node, such as folder,
             * the lookup content may has been destroyed before node destroyed.
             */
            AnalysisContents contents = NetBeansPersistenceManager.getOccupiedContents(node);
            if (contents != null) {
                int confirm = JOptionPane.YES_OPTION;
                if (shouldConfirm) {
                    confirm = JOptionPane.showConfirmDialog(
                            WindowManager.getDefault().getMainWindow(),
                            "Are you sure you want to clear data of : " + contents.getUniSymbol() + " ?",
                            "Clearing data ...",
                            JOptionPane.YES_NO_OPTION);
                }
                if (confirm == JOptionPane.YES_OPTION) {
                    String symbol = contents.getUniSymbol();
                    /** drop tables in database */
                    PersistenceManager.getDefault().dropAllQuoteTables(symbol);
                }
            }
        }
        
        public void execute() {
            perform(true);
        }
        
    }
    
    private static class SymbolReimportDataAction extends GeneralAction {
        
        private Node node;
        
        SymbolReimportDataAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Reimport Data");
        }
        
        public void execute() {
            /** is this a folder ? if true, go recursively */
            if (node.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : node.getChildren().getNodes()) {
                    child.getLookup().lookup(SymbolReimportDataAction.class).execute();
                }
                return;
            }
            
            
            /** otherwise, it's an OneSymbolNode, do real things */
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
            
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            
            calendar.setTime(quoteContract.getBegDate());
            long fromTime = calendar.getTimeInMillis();
            
            Frequency freq = quoteContract.getFreq();
            PersistenceManager.getDefault().deleteQuotes(contents.getUniSymbol(), freq, fromTime, Long.MAX_VALUE);
            
            Sec sec = (Sec)contents.getSerProvider();
            if (sec == null) {
                sec = new Stock(contents.getUniSymbol(), quoteContract);
                contents.setSerProvider(sec);
            } else {
                sec.setDataContract(quoteContract);
            }
            
            /**
             * @TODO
             * need more works, the clear(long) in default implement of Ser doesn't work good!
             */
            sec.clearSer(freq);
            
            node.getLookup().lookup(ViewAction.class).execute();
        }
        
    }
    
    private static class SymbolRefreshDataAction extends GeneralAction {
        
        private Node node;
        
        SymbolRefreshDataAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Refresh Data");
        }
        
        public void execute() {
            /** is this a folder ? if true, go recursively */
            if (node.getLookup().lookup(DataFolder.class) != null) {
                for (Node child : node.getChildren().getNodes()) {
                    child.getLookup().lookup(SymbolRefreshDataAction.class).execute();
                }
                return;
            }
            
            /** otherwise, it's an OneSymbolNode, do real things */
            
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
            
            Sec sec = (Sec)contents.getSerProvider();
            if (sec == null) {
                sec = new Stock(contents.getUniSymbol(), quoteContract);
                contents.setSerProvider(sec);
            } else {
                sec.setDataContract(quoteContract);
            }
            
            sec.clearSer(quoteContract.getFreq());
            
            node.getLookup().lookup(ViewAction.class).execute();
        }
        
    }
    
    private static class SymbolSetDataSourceAction extends GeneralAction {
        
        private Node node;
        
        SymbolSetDataSourceAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Set Data Source");
        }
        
        public void execute() {
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            
            ImportSymbolDialog pane = new ImportSymbolDialog(
                    WindowManager.getDefault().getMainWindow(),
                    contents.lookupActiveDescriptor(QuoteContract.class),
                    false);
            if (pane.showDialog() != JOptionPane.OK_OPTION) {
                return;
            }
            
            contents.lookupAction(SaveAction.class).execute();
            node.getLookup().lookup(SymbolReimportDataAction.class).execute();
        }
        
    }
    
    private static class SymbolCompareToAction extends GeneralAction {
        
        private Node node;
        private AnalysisChartTopComponent analysisTc;
        private Sec sec;
        private SerChangeListener listener;
        
        SymbolCompareToAction(Node node) {
            this.node = node;
            putValue(Action.NAME, "Compare to Current");
        }
        
        public void execute() {
            AnalysisContents contents = node.getLookup().lookup(AnalysisContents.class);
            QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
            
            sec = (Sec)contents.getSerProvider();
            if (sec == null) {
                sec = new Stock(contents.getUniSymbol(), quoteContract);
                contents.setSerProvider(sec);
            }
            
            analysisTc = AnalysisChartTopComponent.getSelected();
            
            if (!sec.isSerLoaded(quoteContract.getFreq())) {
                boolean loadBegins = sec.loadSer(quoteContract.getFreq());
            }
            
            if (analysisTc != null) {
                QuoteSer serToBeCompared = sec.getSer(quoteContract.getFreq());
                ChartViewContainer viewContainer = analysisTc.lookupViewContainer(serToBeCompared.getFreq());
                
                if (viewContainer == null) {
                    return;
                }
                
                Ser baseSer = viewContainer.getController().getMasterSer();
                QuoteCompareIndicator quoteCompareIndicator = new QuoteCompareIndicator(baseSer);
                quoteCompareIndicator.setShortDescription(sec.getUniSymbol());
                quoteCompareIndicator.setSerToBeCompared(serToBeCompared);
                quoteCompareIndicator.computeFrom(0);
                
                viewContainer.getController().scrollReferCursorToLeftSide();
                ((AnalysisQuoteChartView)viewContainer.getMasterView()).addQuoteCompareChart(quoteCompareIndicator);
                
                analysisTc.setSelectedViewContainer(viewContainer);
                analysisTc.requestActive();
            }
            
        }
        
    }
    
    /** Creating an action for adding a folder to organize stocks into groups */
    private static class AddFolderAction extends AbstractAction {
        private DataFolder folder;
        
        public AddFolderAction(DataFolder df) {
            folder = df;
            putValue(Action.NAME, NbBundle.getMessage(SymbolNode.class, "SN_addfolderbutton"));
        }
        
        public void actionPerformed(ActionEvent ae) {
            String floderName = JOptionPane.showInputDialog(
                    WindowManager.getDefault().getMainWindow(),
                    "Please Input Folder Name",
                    "Add Folder",
                    JOptionPane.OK_CANCEL_OPTION);
            
            if (floderName == null) {
                return;
            }
            
            floderName = floderName.trim();
            
            try {
                DataFolder.create(folder, floderName);
            } catch (IOException ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
    }
    
    
    /** Deserialize a Symbol from xml file */
    private static AnalysisContents readContents(Node node) {
        XMLDataObject xdo = node.getLookup().lookup(XMLDataObject.class);
        if (xdo == null) {
            throw new IllegalStateException("Bogus file in Symbols folder: " + node.getLookup().lookup(FileObject.class));
        }
        FileObject readFrom = xdo.getPrimaryFile();
        try {
            InputStream is = readFrom.getInputStream();
            XMLReader xmlReader = XMLUtil.createXMLReader();
            ContentsParseHandler handler = new ContentsParseHandler();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(is));
            
            return handler.getContents();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
        } catch (SAXException ex) {
            ErrorManager.getDefault().notify(ex);
        }
        
        return null;
    }
    
}
