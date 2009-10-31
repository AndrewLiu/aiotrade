package org.aiotrade.applet;

import com.nyapc.aiotrade.dataserver.ApcQuoteServer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import org.aiotrade.lib.charting.chart.QuoteChart;
import org.aiotrade.lib.indicator.IndicatorDescriptor;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.charting.laf.Willa;
import org.aiotrade.lib.charting.view.ChartingController;
import org.aiotrade.lib.charting.view.ChartingControllerFactory;
import org.aiotrade.lib.math.timeseries.TFreq;
import org.aiotrade.lib.math.timeseries.QuoteSer;
import org.aiotrade.lib.math.timeseries.datasource.SerProvider;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.plaf.AIOTabbedPaneUI;
import org.aiotrade.platform.core.analysis.chartview.AnalysisChartViewContainer;
import org.aiotrade.platform.core.analysis.chartview.AnalysisQuoteChartView;
import org.aiotrade.platform.core.analysis.chartview.RealTimeBoardPanel;
import org.aiotrade.platform.core.analysis.chartview.RealTimeChartViewContainer;
import org.aiotrade.lib.indicator.VOLIndicator;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.dataserver.TickerContract;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Market;
import org.aiotrade.platform.core.sec.Sec;
import org.aiotrade.platform.core.sec.Stock;
import org.aiotrade.platform.core.sec.TickerSnapshot;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooQuoteServer;
import org.aiotrade.platform.modules.indicator.basic.MAIndicator;
import org.aiotrade.platform.modules.indicator.basic.RSIIndicator;

/**
 *
 * @author Caoyuan Deng
 */
public class Util {

    private static final Locale[] EAST_REGIONS= {Locale.CHINA, Locale.TAIWAN, Locale.JAPAN, Locale.KOREA};

    private static ResourceBundle BUNDLE = ResourceBundle.getBundle("org.aiotrade.applet.Bundle");
    public static Set<Reference<AnalysisChartViewContainer>> viewContainers = new HashSet<Reference<AnalysisChartViewContainer>>();
    static LookFeel WILLA = new Willa();
    // 544 width is the proper size to fit 2 pixes 241 bar (one trading day's 1min)
    private static int MAIN_PANE_WIDTH = 544;
    private TickerServer tickerServer;
    private Sec sec;
    private static URL BASE_URL;

    public Util() {
    }

    public static void setBaseUrl(URL baseUrl) {
        BASE_URL = baseUrl;
    }

    public static URL getBaseUrl() {
        return BASE_URL;
    }

    /***
     * @param para parameters defined
     * @param data in previsous format. current only dailly supported.
     * @return a image
     */
    public Collection<Reference<AnalysisChartViewContainer>> drawChart(
            Container pane,
            int width, int height,
            String symbol,
            String category,
            String sname,
            Class quoteServer,
            Class tickerServer) {

        Locale locale = Locale.getDefault();

        LookFeel laf = WILLA;
        laf.setAntiAlias(true);
        for (Locale localex: EAST_REGIONS) {
            if (localex.getCountry().equals(locale.getCountry())) {
                laf.setPositiveNegativeColorReversed(true);
                break;
            }
        }
        LookFeel.setCurrent(laf);

        setUIStyle();

        Frequency freqOneMin = Frequency.ONE_MIN;
        Frequency freqDaily = Frequency.DAILY;

        Set<QuoteContract> quoteContracts = new HashSet<QuoteContract>();
        QuoteContract dailyQuoteContract = createQuoteContract(symbol, category, sname, freqDaily, false, quoteServer);
        quoteContracts.add(dailyQuoteContract);
        boolean supportOneMin = dailyQuoteContract.isFreqSupported(freqOneMin);
        QuoteContract oneMinQuoteContract = createQuoteContract(symbol, category, sname, freqOneMin, supportOneMin, quoteServer);
        quoteContracts.add(oneMinQuoteContract);
        TickerContract tickerContract = null;
        if (tickerServer != null) {
            tickerContract = createTickerContract(symbol, category, sname, freqOneMin, tickerServer);
        }

        sec = new Stock(symbol, quoteContracts, tickerContract);
        Market market;
        if (quoteServer.getName().equals(YahooQuoteServer.class.getName())) {
            market = YahooQuoteServer.GetMarket(symbol);
        } else {
            market = ApcQuoteServer.GetMarket(symbol);
        }
        sec.setMarket(market);

        AnalysisContents dailyContents = createAnalysisContents(symbol, freqDaily, quoteServer, tickerServer);
        dailyContents.addDescriptor(dailyQuoteContract);
        dailyContents.setSerProvider(sec);
        loadSer(dailyContents);

        AnalysisContents rtContents = createAnalysisContents(symbol, freqOneMin, quoteServer, tickerServer);
        rtContents.addDescriptor(oneMinQuoteContract);
        rtContents.setSerProvider(sec);
        loadSer(rtContents);

        // --- other freqs:
        AnalysisChartViewContainer oneMinViewContainer = createViewContainer(
                sec.getSer(freqOneMin),
                rtContents,
                symbol,
                QuoteChart.Type.Line,
                pane);

        oneMinViewContainer.setPreferredSize(new Dimension(width, height));
        viewContainers.add(new WeakReference<AnalysisChartViewContainer>(oneMinViewContainer));

        AnalysisChartViewContainer dailyViewContainer = createViewContainer(
                sec.getSer(freqDaily),
                dailyContents,
                symbol,
                QuoteChart.Type.Candle,
                pane);
        dailyViewContainer.setPreferredSize(new Dimension(width, height));
        viewContainers.add(new WeakReference<AnalysisChartViewContainer>(dailyViewContainer));

        RealTimeChartViewContainer rtViewContainer = createRealTimeViewContainer(sec, rtContents, pane);

        try {
            pane.setLayout(new BorderLayout());
            pane.setLayout(new BorderLayout());

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setFocusable(false);
            splitPane.setBackground(Color.WHITE);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            splitPane.setOneTouchExpandable(false);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(MAIN_PANE_WIDTH);

            //pane.add(BorderLayout.NORTH, createToolBar(width));
            pane.add(BorderLayout.CENTER, splitPane);

            JTabbedPane tabbedPane = createTabbedPane();
            tabbedPane.setFocusable(false);

            //tabbedPane.setBorder(new AIOScrollPaneStyleBorder(LookFeel.getCurrent().borderColor));

            splitPane.add(JSplitPane.LEFT, tabbedPane);

            RealTimeBoardPanel rtBoard = new RealTimeBoardPanel(sec, rtContents);
            Box rtBoardBoxV = Box.createVerticalBox();
            rtBoardBoxV.add(Box.createVerticalStrut(22)); // use to align top of left pane's content pane
            rtBoardBoxV.add(rtBoard);

            Box rtBoardBoxH = Box.createHorizontalBox();
            rtBoardBoxH.add(Box.createHorizontalStrut(5));
            rtBoardBoxH.add(rtBoardBoxV);

            splitPane.add(JSplitPane.RIGHT, rtBoardBoxH);

            JPanel rtPanel = new JPanel(new BorderLayout());
            rtPanel.add(BorderLayout.CENTER, rtViewContainer);

            JPanel oneMinPanel = new JPanel(new BorderLayout());
            oneMinPanel.add(BorderLayout.CENTER, oneMinViewContainer);

            JPanel dailyPanel = new JPanel(new BorderLayout());
            dailyPanel.add(BorderLayout.CENTER, dailyViewContainer);

            tabbedPane.addTab(BUNDLE.getString("realTime"), rtPanel);
            tabbedPane.addTab(BUNDLE.getString("oneMin"), oneMinPanel);
            tabbedPane.addTab(BUNDLE.getString("daily"), dailyPanel);

            watchRealTime(rtContents, rtBoard);
            //container.getController().setCursorCrossLineVisible(showLastClose(apcpara));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return viewContainers;
    }

//    private JToolBar createToolBar(int width) {
//        JToolBar toolBar = new JToolBar();
//        toolBar.add(new ZoomInAction());
//        toolBar.add(new ZoomOutAction());
//
//        toolBar.setPreferredSize(new Dimension(width, 18));
//        for (Component c : toolBar.getComponents()) {
//            c.setFocusable(false);
//        }
//
//        return toolBar;
//    }

    private QuoteContract createQuoteContract(String symbol, String category, String sname, Frequency freq, boolean refreshable, Class server) {
        QuoteContract dataContract = new QuoteContract();

        dataContract.setActive(true);
        dataContract.setServiceClassName(server.getName());

        dataContract.setSymbol(symbol);
        dataContract.setCategory(category);
        dataContract.setShortName(sname);
        dataContract.setSecType(Sec.Type.Stock);
        dataContract.setExchange("SSH");
        dataContract.setPrimaryExchange("SSH");
        dataContract.setCurrency("USD");

        dataContract.setDateFormatString("yyyy-MM-dd-HH-mm");

        dataContract.setFreq(freq);

        dataContract.setRefreshable(refreshable);
        dataContract.setRefreshInterval(5);

        return dataContract;
    }

    private static TickerContract createTickerContract(String symbol, String category, String sname, Frequency freq, Class server) {
        TickerContract dataContract = new TickerContract();

        dataContract.setActive(true);
        dataContract.setServiceClassName(server.getName());

        dataContract.setSymbol(symbol);
        dataContract.setCategory(category);
        dataContract.setShortName(sname);
        dataContract.setSecType(Sec.Type.Stock);
        dataContract.setExchange("SSH");
        dataContract.setPrimaryExchange("SSH");
        dataContract.setCurrency("USD");

        dataContract.setDateFormatString("yyyy-MM-dd-HH-mm-ss");
        dataContract.setFreq(freq);
        dataContract.setRefreshable(true);
        dataContract.setRefreshInterval(5);

        return dataContract;
    }

    private final AnalysisContents createAnalysisContents(String symbol, Frequency freq, Class quoteServer, Class tickerServer) {
        AnalysisContents contents = new AnalysisContents(symbol);

        contents.addDescriptor(createIndicatorDescriptor(MAIndicator.class, freq));
        contents.addDescriptor(createIndicatorDescriptor(VOLIndicator.class, freq));
        contents.addDescriptor(createIndicatorDescriptor(RSIIndicator.class, freq));

        return contents;
    }

//    private static final AnalysisContents createRealTimeContents(String symbol, Frequency freq, Class quoteServer) {
//        AnalysisContents contents = new AnalysisContents(symbol);
//
//        contents.addDescriptor(createIndicatorDescriptor(VOLIndicator.class, freq));
//
//        QuoteContract quoteContract = createQuoteContract(symbol, freq, quoteServer);
//        TickerContract tickerContract = createTickerContract(symbol, ApcTickerServer.class);
//        contents.addDescriptor(quoteContract);
//        contents.addDescriptor(tickerContract);
//
//        return contents;
//    }
    private static void loadSer(AnalysisContents contents) {
        QuoteContract quoteContract = contents.lookupActiveDescriptor(QuoteContract.class);

        Frequency freq = quoteContract.getFreq();
        if (!quoteContract.isFreqSupported(freq)) {
            return;
        }

        SerProvider sec = contents.getSerProvider();
        boolean mayNeedsReload = false;
        if (sec == null) {
            return;
        } else {
            mayNeedsReload = true;
        }

        if (mayNeedsReload) {
            sec.clearSer(freq);
        }

        if (!sec.isSerLoaded(freq)) {
            sec.loadSer(freq);
        }
        return;
    }

    private IndicatorDescriptor createIndicatorDescriptor(Class clazz, Frequency frenquency) {
        IndicatorDescriptor indicator = new IndicatorDescriptor();
        indicator.setActive(true);
        indicator.setServiceClassName(clazz.getName());
        indicator.setFreq(frenquency);
        return indicator;
    }

    private AnalysisChartViewContainer createViewContainer(
            QuoteSer ser,
            AnalysisContents contents,
            String title,
            QuoteChart.Type type,
            Component parent) {

        ChartingController controller =
                ChartingControllerFactory.createInstance(ser, contents);
        AnalysisChartViewContainer viewContainer =
                controller.createChartViewContainer(AnalysisChartViewContainer.class, parent);

        if (title == null) {
            title = ser.getFreq().getName();
        }
        title = new StringBuilder(" ").append(title).append(" ").toString();

        viewContainer.getController().setCursorCrossLineVisible(true);
        viewContainer.getController().setOnCalendarMode(false);
        AnalysisQuoteChartView masterView = (AnalysisQuoteChartView) viewContainer.getMasterView();
        masterView.switchQuoteChartType(type);
        masterView.getXControlPane().setVisible(true);
        masterView.getYControlPane().setVisible(true);

        /** inject popup menu from this TopComponent */
        //viewContainer.setComponentPopupMenu(popupMenuForViewContainer);
        return viewContainer;
    }

    private RealTimeChartViewContainer createRealTimeViewContainer(Sec sec, AnalysisContents contents, Component parent) {
        QuoteSer masterSer = sec.getSer(Frequency.ONE_MIN);
        if (masterSer == null) {
            masterSer = sec.getTickerSer();
        }
        ChartingController controller = ChartingControllerFactory.createInstance(
                masterSer, contents);
        RealTimeChartViewContainer viewContainer = controller.createChartViewContainer(
                RealTimeChartViewContainer.class, parent);
        return viewContainer;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFocusable(false);
        return tabbedPane;
//        tabbedPane.addChangeListener(new ChangeListener() {
//            private Color selectedColor = new Color(177, 193, 209);
//
//            public void stateChanged(ChangeEvent e) {
//                JTabbedPane tp = (JTabbedPane)e.getSource();
//
//                for (int i = 0; i < tp.getTabCount(); i++) {
//                    tp.setBackgroundAt(i, null);
//                }
//                int idx = tp.getSelectedIndex();
//                tp.setBackgroundAt(idx, selectedColor);
//
//                //updateToolbar();
//
//                if (tp.getSelectedComponent() instanceof AnalysisChartViewContainer) {
//                    AnalysisChartViewContainer viewContainer = (AnalysisChartViewContainer)tp.getSelectedComponent();
//                    MasterSer masterSer = viewContainer.getController().getMasterSer();
//
//                    /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */
//
//                    Node secNode = NetBeansPersistenceManager.getOccupantNode(contents);
//                    assert secNode != null : "There should be at least one created node bound with descriptors here, as view has been opened!";
//                    for (Node groupNode : secNode.getChildren().getNodes()) {
//                        ((GroupNode)groupNode).setTimeFrequency(masterSer.getFreq());
//                    }
//
//                    /** update the supportedFreqsComboBox */
//                    setSelectedFreqItem(masterSer.getFreq());
//                }
//            }
//        });
//
    }

    private void setUIStyle() {
//        UIDefaults defs = UIManager.getDefaults();
//        Enumeration keys = defs.keys();
//        while (keys.hasMoreElements()) {
//            Object key = keys.nextElement();
//            if (key.toString().startsWith("TabbedPane")) {
//                System.out.println(key);
//            }
//        }

        UIManager.put("TabbedPaneUI", AIOTabbedPaneUI.class.getName());
        /** get rid of the ugly border of JTabbedPane: */
//        Insets oldInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
        /*- set top insets as 1 for TOP placement if you want:
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
         */
        //UIManager.getColor("TabbedPane.tabAreaBackground");
        UIManager.put("TabbedPane.selected", LookFeel.getCurrent().backgroundColor);
        UIManager.put("TabbedPane.selectHighlight", LookFeel.getCurrent().backgroundColor);

        UIManager.put("TabbedPane.unselectedBackground", Color.WHITE);
        UIManager.put("TabbedPane.selectedBorderColor", LookFeel.getCurrent().borderColor);
//        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 1));
//        UIManager.put("TabbedPane.contentBorderInsets", oldInsets);
//        UIManager.put("TabbedPane.font", new Font("Dialog", Font.PLAIN, 11));
//        UIManager.put("TabbedPane.foreground", LookFeel.getCurrent().borderColor);
//        UIManager.put("TabbedPane.background", Color.WHITE);
//        UIManager.put("TabbedPane.shadow", Color.GRAY);
//        UIManager.put("TabbedPane.darkShadow", Color.GRAY);
    }

    public void watchRealTime(AnalysisContents contents, RealTimeBoardPanel rtBoard) {
        sec.subscribeTickerServer();

        tickerServer = sec.getTickerServer();
        if (tickerServer == null) {
            return;
        }

        TickerSnapshot tickerSnapshot = tickerServer.getTickerSnapshot(sec.getTickerContract().getSymbol());
        if (tickerSnapshot != null) {
            tickerSnapshot.addObserver(rtBoard);
        }
    }

    public BufferedImage paintToImage(
            AnalysisChartViewContainer container,
            ChartingController controller,
            long begTime,
            long endTime,
            int width,
            int height,
            JFrame fm) throws Exception {

        container.setPreferredSize(new Dimension(width, height));

        container.setBounds(0, 0, width, height);

        fm.getContentPane().add(container);
        fm.pack();

        int begPos = controller.getMasterSer().rowOfTime(begTime);
        int endPos = controller.getMasterSer().rowOfTime(endTime);
        int nBars = endPos - begPos + 1;

        // wViewport should minus AxisYPane's width
        int wViewPort = width - container.getMasterView().getAxisYPane().getWidth();
        controller.setWBarByNBars(wViewPort, nBars);


//        /** backup: */
        int backupRightCursorPos = controller.getRightSideRow();
        int backupReferCursorPos = controller.getReferCursorRow();

        controller.setCursorByRow(backupReferCursorPos, endPos - 1, true);

        container.paintToImage();

        return (BufferedImage) container.paintToImage();
        /** restore: */
        //controller.setCursorByRow(backupReferCursorPos, backupRightCursorPos);
    }

    public void releaseAll() {
        // Since ticker server is singleton, will be reused in browser, should unSubscribe it to get tickerSnapshot etc to be reset
        sec.unSubscribeTickerServer();
    }
}
