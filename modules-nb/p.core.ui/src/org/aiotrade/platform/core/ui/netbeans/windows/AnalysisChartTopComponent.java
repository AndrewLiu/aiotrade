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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.math.timeseries.MasterSer;
import org.aiotrade.charting.view.ChartView;
import org.aiotrade.charting.view.ChartViewContainer;
import org.aiotrade.platform.core.analysis.chartview.AnalysisChartViewContainer;
import org.aiotrade.charting.view.ChartingController;
import org.aiotrade.charting.view.ChartingControllerFactory;
import org.aiotrade.charting.view.WithDrawingPane;
import org.aiotrade.charting.view.pane.DrawingPane;
import org.aiotrade.charting.descriptor.DrawingDescriptor;
import org.aiotrade.math.timeseries.Unit;
import org.aiotrade.math.timeseries.QuoteSerCombiner;
import org.aiotrade.platform.core.netbeans.NetBeansPersistenceManager;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchHideShowDrawingLineAction;
import org.aiotrade.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.charting.descriptor.IndicatorDescriptor;
import org.aiotrade.math.timeseries.computable.Indicator;
import org.aiotrade.math.timeseries.QuoteSer;
import org.aiotrade.math.timeseries.Ser;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.ui.netbeans.actions.ChangeOptsAction;
import org.aiotrade.platform.core.ui.netbeans.actions.ChangeStatisticChartOptsAction;
import org.aiotrade.platform.core.ui.netbeans.actions.PickIndicatorAction;
import org.aiotrade.platform.core.ui.netbeans.actions.RemoveCompareQuoteChartsAction;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchAdjustQuoteAction;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchCandleOhlcAction;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchLinearLogScaleAction;
import org.aiotrade.platform.core.ui.netbeans.actions.SwitchCalendarTradingTimeViewAction;
import org.aiotrade.platform.core.ui.netbeans.actions.ZoomInAction;
import org.aiotrade.platform.core.ui.netbeans.actions.ZoomOutAction;
import org.aiotrade.platform.core.ui.netbeans.explorer.GroupNode;
import org.aiotrade.charting.laf.LookFeel;
import org.aiotrade.util.ReferenceOnly;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;


/** This class implements serializbale by inheriting TopComponent, but should
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
public class AnalysisChartTopComponent extends TopComponent {
    private final static List<WeakReference<AnalysisChartTopComponent>> instanceRefs =
            new ArrayList<WeakReference<AnalysisChartTopComponent>>();
    private WeakReference<AnalysisChartTopComponent> ref;
    
    /** The Mode this component will live in. */
    private static final String MODE = "editor";
    
    private String s_id = "";
    
    private String symbol;
    
    private JPanel tabbedPaneContainer;
    private JTabbedPane tabbedPane;
    private JComboBox supportedFreqsComboBox;
    
    private ChartViewContainer defaultViewContainer;
    private Map<Frequency, ChartViewContainer> freqMapViewContainer = 
            new LinkedHashMap<Frequency, ChartViewContainer>();
    
    private Sec sec;
    private AnalysisContents contents;
    private QuoteContract quoteContract;
    
    private JPopupMenu popupMenuForViewContainer;
    
    QuoteSerCombiner weeklyCombiner;
    QuoteSerCombiner monthlyCombiner;
    
    public AnalysisChartTopComponent(Sec sec, AnalysisContents contents) {
        this.sec = sec;
        this.contents = contents;
        this.quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);
        
        ref = new WeakReference<AnalysisChartTopComponent>(this);
        instanceRefs.add(ref);
        
        injectActionsToDescriptors();
        injectActionsToPopupMenuForViewContainer();
        
        initSec();
        initComponents();
    }
    
    private void injectActionsToDescriptors() {
        /** we choose here to lazily create actions instances */
        
        /** init all children of node to create the actions that will be injected to descriptor */
//        final Node node = NetBeansPersistenceManager.getOccupantNode(contents);
//        initNodeChildrenRecursively(node);
    }
    
    private final void initNodeChildrenRecursively(Node node) {
        if (! node.isLeaf()) {
            /** call getChildren().getNodes(true) to initialize all children nodes */
            final Node[] childrenNodes = node.getChildren().getNodes(true);
            for (Node _node : childrenNodes) {
                initNodeChildrenRecursively(_node);
            }
        }
    }
    
    private void injectActionsToPopupMenuForViewContainer() {
        popupMenuForViewContainer = new JPopupMenu();
        popupMenuForViewContainer.add(SystemAction.get(SwitchCandleOhlcAction.class));
        popupMenuForViewContainer.add(SystemAction.get(SwitchCalendarTradingTimeViewAction.class));
        popupMenuForViewContainer.add(SystemAction.get(SwitchLinearLogScaleAction.class));
        popupMenuForViewContainer.add(SystemAction.get(SwitchAdjustQuoteAction.class));
        popupMenuForViewContainer.add(SystemAction.get(ZoomInAction.class));
        popupMenuForViewContainer.add(SystemAction.get(ZoomOutAction.class));
        popupMenuForViewContainer.addSeparator();
        popupMenuForViewContainer.add(SystemAction.get(PickIndicatorAction.class));
        popupMenuForViewContainer.add(SystemAction.get(ChangeOptsAction.class));
        popupMenuForViewContainer.addSeparator();
        popupMenuForViewContainer.add(SystemAction.get(ChangeStatisticChartOptsAction.class));
        popupMenuForViewContainer.addSeparator();
        popupMenuForViewContainer.add(SystemAction.get(RemoveCompareQuoteChartsAction.class));
    }
    
    private void initSec() {
        symbol = sec.getUniSymbol();
        this.s_id = sec.getName();
        
        if (!sec.isSerLoaded(quoteContract.getFreq())) {
            sec.loadSer(quoteContract.getFreq());
        }
    }
    
    private void initComponents() {
        setFont(LookFeel.getCurrent().axisFont);
        
        createTabbedPane();
        createSupportedFreqsComboBox();
        
        defaultViewContainer = addViewContainer(sec.getSer(quoteContract.getFreq()), contents, null);
        
        if (quoteContract.getFreq().unit == Unit.Day) {
            createFollowedViewContainers();
        }
        
        tabbedPaneContainer = new JPanel();
        OverlayLayout overlay = new OverlayLayout(tabbedPaneContainer);
        tabbedPaneContainer.setLayout(overlay);
        
        Dimension d = new Dimension(80, 22);
        supportedFreqsComboBox.setPreferredSize(d);
        supportedFreqsComboBox.setMaximumSize(d);
        supportedFreqsComboBox.setMinimumSize(d);
        supportedFreqsComboBox.setFocusable(false);
        /** add supportedFreqsComboBox prior to tabbedPane, so supportedFreqsComboBox can accept mouse input */
        tabbedPaneContainer.add(supportedFreqsComboBox);
        
        tabbedPane.setOpaque(false);
        tabbedPane.setFocusable(true);
        tabbedPaneContainer.add(tabbedPane);
        
        tabbedPane.setAlignmentX(1.0f);
        supportedFreqsComboBox.setAlignmentX(1.0f);
        tabbedPane.setAlignmentY(0.0f);
        supportedFreqsComboBox.setAlignmentY(0.0f);
        
        setLayout(new BorderLayout());
        add(tabbedPaneContainer, BorderLayout.CENTER);
        setName(sec.getName());
        
        /** this component should setFocusable(true) to have the ability to grab the focus */
        setFocusable(true);
        /** as the NetBeans window system manage focus in a strange manner, we should do: */
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                ChartViewContainer selectedViewContainer = getSelectedViewContainer();
                if (selectedViewContainer != null) {
                    selectedViewContainer.requestFocusInWindow();
                }
            }
            
            public void focusLost(FocusEvent e) {
            }
        });
        
        //tabbedPane.setFocusable(false);
        //FocusOwnerChecker check = new FocusOwnerChecker();
    }
    
    private void createTabbedPane() {
        /** get rid of the ugly border of JTabbedPane: */
        Insets oldInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
        /*- set top insets as 1 for TOP placement if you want:
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
         */
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 1));
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        UIManager.put("TabbedPane.contentBorderInsets", oldInsets);
        
        tabbedPane.addChangeListener(new ChangeListener() {
            private Color selectedColor = new Color(177, 193, 209);
            
            public void stateChanged(ChangeEvent e) {
                JTabbedPane tp = (JTabbedPane)e.getSource();
                
                for (int i = 0; i < tp.getTabCount(); i++) {
                    tp.setBackgroundAt(i, null);
                }
                int idx = tp.getSelectedIndex();
                tp.setBackgroundAt(idx, selectedColor);
                
                updateToolbar();
                
                if (tp.getSelectedComponent() instanceof AnalysisChartViewContainer) {
                    AnalysisChartViewContainer viewContainer = (AnalysisChartViewContainer)tp.getSelectedComponent();
                    MasterSer masterSer = viewContainer.getController().getMasterSer();
                    
                    /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */
                    
                    Node secNode = NetBeansPersistenceManager.getOccupantNode(contents);
                    assert secNode != null : "There should be at least one created node bound with descriptors here, as view has been opened!";
                    for (Node groupNode : secNode.getChildren().getNodes()) {
                        ((GroupNode)groupNode).setTimeFrequency(masterSer.getFreq());
                    }
                    
                    /** update the supportedFreqsComboBox */
                    setSelectedFreqItem(masterSer.getFreq());
                }
            }
        });
        
    }
    
    private void createSupportedFreqsComboBox() {
        Frequency[] supportedFreqs = null;
        
        supportedFreqs = quoteContract.getSupportedFreqs();
        if (supportedFreqs == null) {
            /**
             * this quoteContract supports customed frequency (as CSV), we don't
             * know how freqs it may support, so, just add the default one
             * @see QuoteSourceDescriptor#getSupportedFreqs()
             */
            supportedFreqs = new Frequency[] {quoteContract.getFreq()};
        }
        
        supportedFreqsComboBox = new JComboBox();
        supportedFreqsComboBox.setModel(new DefaultComboBoxModel(supportedFreqs));
        supportedFreqsComboBox.setFocusable(false);
        supportedFreqsComboBox.setEditable(true);
        supportedFreqsComboBox.setSelectedItem(quoteContract.getFreq());
        
        supportedFreqsComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                /**
                 * change a item may cause two times itemStateChanged, the old one
                 * will get the ItemEvent.DESELECTED and the new item will get the
                 * ItemEvent.SELECTED. so, should check the affected item first:
                 */
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                
                Frequency freq = (Frequency)e.getItem();
                ChartViewContainer viewContainer = lookupViewContainer(freq);
                if (viewContainer != null) {
                    setSelectedViewContainer(viewContainer);
                } else {
                    QuoteSer ser = sec.getSer(freq);
                    if (ser == null) {
                        boolean loadBeginning = sec.loadSer(freq);
                        if (loadBeginning) {
                            ser = sec.getSer(freq);
                        }
                    }
                    
                    if (ser != null) {
                        viewContainer = addViewContainer(ser, contents, null);
                        setSelectedViewContainer(viewContainer);
                    }
                }
            }
        });
    }
    
    /**
     * !NOTICE
     * here should be aware that if sec's ser has been loaded, no more
     * SerChangeEvent.Type.FinishedLoading will be fired, so if we create followed
     * viewContainers here, should make sure that the QuoteSerCombiner listen
     * to SeriesChangeEvent.FinishingCompute or SeriesChangeEvent.FinishingLoading from
     * sec's ser and computeFrom(0) at once.
     */
    private void createFollowedViewContainers() {
        QuoteSer dailySer = sec.getSer(quoteContract.getFreq());
        QuoteSer weeklySer = new QuoteSer(Frequency.WEEKLY);
        weeklyCombiner = new QuoteSerCombiner(dailySer, weeklySer);
        weeklyCombiner.computeFrom(0); // don't remove me, see notice above.
        sec.putSer(weeklySer);
        addViewContainer(weeklySer, contents, null);
        
        QuoteSer monthlySer = new QuoteSer(Frequency.MONTHLY);
        monthlyCombiner = new QuoteSerCombiner(dailySer, monthlySer);
        monthlyCombiner.computeFrom(0); // don't remove me, see notice above.
        sec.putSer(monthlySer);
        addViewContainer(monthlySer, contents, null);
        
        QuoteSer tickerSer = sec.getTickerSer();
        if (quoteContract.isFreqSupported(tickerSer.getFreq())) {
            sec.loadSer(tickerSer.getFreq());
        }
        addViewContainer(tickerSer, contents, null);
    }
    
    private AnalysisChartViewContainer addViewContainer(QuoteSer ser, AnalysisContents contents, String title) {
        ChartingController controller = ChartingControllerFactory.createInstance(
                ser, contents);
        AnalysisChartViewContainer viewContainer = controller.createChartViewContainer(
                AnalysisChartViewContainer.class, this);
        
        if (title == null) {
            title = ser.getFreq().getName();
        }
        title = new StringBuilder(" ").append(title).append(" ").toString();
        
        tabbedPane.addTab(title, viewContainer);
        
        freqMapViewContainer.put(ser.getFreq(), viewContainer);
        
        /** inject popup menu from this TopComponent */
        viewContainer.setComponentPopupMenu(popupMenuForViewContainer);
        
        return viewContainer;
    }
    
    public ChartViewContainer getSelectedViewContainer() {
        if (tabbedPane.getSelectedComponent() instanceof ChartViewContainer) {
            return (ChartViewContainer)tabbedPane.getSelectedComponent();
        }
        
        return null;
    }
    
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    
    public void setSelectedViewContainer(ChartViewContainer viewContainer) {
        /** check to avoid recursively call between tabbedPane and comboBox */
        if (viewContainer != getSelectedViewContainer()) {
            tabbedPane.setSelectedComponent(viewContainer);
        }
    }
    
    public void setSelectedFreqItem(Frequency freq) {
        if (!supportedFreqsComboBox.getSelectedItem().equals(freq)) {
            supportedFreqsComboBox.setSelectedItem(freq);
        }
    }
    
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(MODE);
        /**
         * !NOTICE
         * mode.dockInto(this) seems will close this at first if this.isOpened()
         * So, when call open(), try to check if it was already opened, if true,
         * no need to call open() again
         */
        mode.dockInto(this);
        super.open();
    }
    
    protected void componentActivated() {
        super.componentActivated();
        updateToolbar();
    }
    
    protected void componentShowing() {
        super.componentShowing();
    }
    
    protected void componentClosed() {
        sec.stopAllDataServer();
        
        instanceRefs.remove(ref);
        super.componentClosed();
        /**
         * componentClosed not means it will be discarded, just make it invisible,
         * so, when to call dispose() ?
         */
        //sec.setSignSeriesLoaded(false);
    }
    
    protected String preferredID() {
        if (defaultViewContainer != null) {
            defaultViewContainer.requestFocusInWindow();
        }
        
        return s_id;
    }
    
    public int getPersistenceType() {
        return this.PERSISTENCE_NEVER;
    }
    
    public Sec getStock() {
        return sec;
    }
    
    public static List<WeakReference<AnalysisChartTopComponent>> getInstanceRefs() {
        return instanceRefs;
    }
    
    public static AnalysisChartTopComponent lookupTopComponent(String symbol) {
        for (WeakReference<AnalysisChartTopComponent> ref : getInstanceRefs()) {
            if (ref.get().getStock().getUniSymbol().equalsIgnoreCase(symbol)) {
                return ref.get();
            }
        }
        
        return null;
    }
    
    public static AnalysisChartTopComponent getSelected() {
        TopComponent tc = TopComponent.getRegistry().getActivated();
        if (tc instanceof AnalysisChartTopComponent) {
            return (AnalysisChartTopComponent)tc;
        } else {
            for (WeakReference<AnalysisChartTopComponent> ref : getInstanceRefs()) {
                if (ref.get().isShowing()) {
                    return ref.get();
                }
            }
        }
        
        return null;
    }
    
    private void updateToolbar() {
        ChartViewContainer selectedViewContainer = getSelectedViewContainer();
        if (selectedViewContainer != null) {
            SwitchCalendarTradingTimeViewAction.updateToolbar(selectedViewContainer);
            SwitchAdjustQuoteAction.updateToolbar(selectedViewContainer);
            SwitchHideShowDrawingLineAction.updateToolbar(selectedViewContainer);
            
            selectedViewContainer.requestFocusInWindow();
        }
    }
    
    public Indicator lookupIndicator(IndicatorDescriptor descriptor) {
        for (ChartViewContainer viewContainer : freqMapViewContainer.values()) {
            Frequency freq = viewContainer.getController().getMasterSer().getFreq();
            if (freq.equals(descriptor.getFreq())) {
                ChartView chartView = viewContainer.lookupChartView(descriptor);
                if (chartView != null) {
                    for (Ser ser : chartView.getAllSers()) {
                        if (ser instanceof Indicator) {
                            Indicator indicator = (Indicator)ser;
                            if (indicator.getClass().getName().equalsIgnoreCase(descriptor.getServiceClassName())) {
                                return indicator;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public DrawingPane lookupDrawing(DrawingDescriptor descriptor) {
        for (ChartViewContainer viewContainer : freqMapViewContainer.values()) {
            Frequency freq = viewContainer.getController().getMasterSer().getFreq();
            if (freq.equals(descriptor.getFreq())) {
                ChartView masterView = (ChartView)viewContainer.getMasterView();
                
                if (masterView instanceof WithDrawingPane) {
                    Map<DrawingDescriptor, DrawingPane> descriptorMapDrawing = 
                            ((WithDrawingPane)masterView).getDescriptorMapDrawing();
                    
                    return descriptorMapDrawing.get(descriptor);
                }
            }
        }
        
        return null;
    }
    
    public ChartViewContainer lookupViewContainer(Frequency freq) {
        return freqMapViewContainer.get(freq);
    }
    
    @Override
    protected void finalize() throws Throwable {
        freqMapViewContainer.clear();
        weeklyCombiner.dispose();
        monthlyCombiner.dispose();
        
        instanceRefs.remove(ref);
        super.finalize();
    }
    
    @ReferenceOnly private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenuForViewContainer.show(this, e.getX(), e.getY());
        }
    }
    
    @ReferenceOnly private class MyPopupMouseListener implements MouseListener {
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
    }
    
    
}
