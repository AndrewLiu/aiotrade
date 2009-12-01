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
package org.aiotrade.platform.core.ui.netbeans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import org.aiotrade.lib.charting.chart.QuoteChart;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.util.swing.action.RefreshAction;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.UserOptionsManager;
import org.aiotrade.platform.core.WindowManager;
import org.aiotrade.platform.core.analysis.ContentsParseHandler;
import org.aiotrade.platform.core.analysis.ContentsPersistenceHandler;
import org.aiotrade.platform.core.sec.Quote;
import org.aiotrade.platform.core.sec.QuotePool;
import org.aiotrade.platform.core.sec.TickerPool;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This class implements persistence via NetBeans Node system
 * Actually it will be an application context
 *
 * @author Caoyuan Deng
 */
public class NetBeansPersistenceManager implements PersistenceManager.I {

    /** @TODO
     * WindowManager.getDefault()
     */
    private final static WindowManager WIN_MAN = new NetBeansWindowManager();
    /**
     * we perfer contents instances long lives in application context, so, don't
     * use weak reference map here.
     */
    private final static Map<AnalysisContents, Node> contentMapOccuptantNode =
            new HashMap<AnalysisContents, Node>();
    private static AnalysisContents defaultContents;
    private static final String TABLE_EXISTS_MARK = Long.toString(Long.MAX_VALUE);
    private static final String SYMBOL_INDEX_TABLE_NAME = "AIOTRADESYMBOLINDEX";
    private QuotePool quotePool = new QuotePool();
    private TickerPool tickerPool = new TickerPool();
    private Properties userOptionsProp;
    private Properties dbProp;
    private String dbUrl;

    public NetBeansPersistenceManager() {
        init();
    }

    private void init() {
        restoreProperties();
        checkAndCreateDatabaseIfNecessary();
    }

    public static Node getOccupantNode(AnalysisContents contents) {
        return contentMapOccuptantNode.get(contents);
    }

    public static AnalysisContents getOccupiedContents(Node node) {
        for (AnalysisContents contents : contentMapOccuptantNode.keySet()) {
            if (contentMapOccuptantNode.get(contents) == node) {
                return contents;
            }
        }

        return null;
    }

    public static AnalysisContents lookupContents(String symbol) {
        for (AnalysisContents contents : contentMapOccuptantNode.keySet()) {
            if (contents.getUniSymbol().equals(symbol)) {
                return contents;
            }
        }

        return null;
    }

    public static void putNode(AnalysisContents contents, Node node) {
        contentMapOccuptantNode.put(contents, node);
    }

    /**
     * Remove node will not remove the contents, we prefer contents instances
     * long lives in application context, so if node is moved to other place, we
     * can just pick a contents from here (if exists) instead of read from xml
     * file, and thus makes the opened topcomponent needn't to referencr to a new
     * created contents instance.
     * So, just do
     * <code>putNode(contents, null)</code>
     */
    public static void removeNode(Node node) {
        /**
         * @NOTICE
         * When move a node from a folder to another folder, a new node could
         * be created first, then the old node is removed. so the nodeMap may
         * has been updated by the new node, and lookupContents(node) will
         * return a null since it lookup via the old node.
         * Check it here
         */
        AnalysisContents contents = getOccupiedContents(node);
        if (contents != null) {
            contentMapOccuptantNode.put(contents, null);
        }
    }

    public final QuotePool getQuotePool() {
        return quotePool;
    }

    public final TickerPool getTickerPool() {
        return tickerPool;
    }

    public AnalysisContents getDefaultContents() {
        if (defaultContents != null) {
            return defaultContents;
        } else {
            defaultContents = restoreContents("Default");
            return defaultContents;
        }
    }

    public void saveContents(AnalysisContents contents) {
        if (contents.getUniSymbol().equalsIgnoreCase("Default")) {
            FileObject defaultContentsFile = FileUtil.getConfigFile("UserOptions/DefaultContents.xml");
            if (defaultContentsFile != null) {
                FileLock lock = null;
                try {
                    lock = defaultContentsFile.lock();

                    PrintStream out = new PrintStream(defaultContentsFile.getOutputStream(lock));
                    out.print(ContentsPersistenceHandler.dumpContents(contents));

                    /** should remember to do out.close() here */
                    out.close();
                } catch (IOException ioe) {
                    ErrorManager.getDefault().notify(ioe);
                } finally {
                    if (lock != null) {
                        lock.releaseLock();
                    }
                }
            }
        } else {
            Node node = contentMapOccuptantNode.get(contents);
            if (node != null) {
                /** refresh node's icon in explorer window */
                Children children = node.getChildren();
                for (Node child : children.getNodes()) {
                    child.getLookup().lookup(RefreshAction.class).execute();
                }

                DataObject dob = node.getLookup().lookup(DataObject.class);
                FileObject writeTo = dob.getPrimaryFile();
                FileLock lock = null;
                try {
                    lock = writeTo.lock();

                    PrintStream out = new PrintStream(writeTo.getOutputStream(lock));
                    out.print(ContentsPersistenceHandler.dumpContents(contents));

                    /** should remember to do out.close() here */
                    out.close();
                } catch (IOException ioe) {
                    ErrorManager.getDefault().notify(ioe);
                } finally {
                    if (lock != null) {
                        lock.releaseLock();
                    }
                }
            }


        }
    }

    public AnalysisContents restoreContents(String symbol) {
        AnalysisContents contents = null;

        if (symbol.equalsIgnoreCase("Default")) {
            FileObject defaultContentsFile = FileUtil.getConfigFile("UserOptions/DefaultContents.xml");
            if (defaultContentsFile != null) {
                try {
                    InputStream is = defaultContentsFile.getInputStream();
                    XMLReader xmlReader = XMLUtil.createXMLReader();
                    ContentsParseHandler handler = new ContentsParseHandler();
                    xmlReader.setContentHandler(handler);
                    xmlReader.parse(new InputSource(is));
                    contents = handler.getContents();

                    is.close();
                } catch (IOException ex) {
                    ErrorManager.getDefault().notify(ex);
                } catch (SAXException ex) {
                    ErrorManager.getDefault().notify(ex);
                }
            }
        } else {
            /** @TODO
             *  useful or useless in this case? */
        }

        return contents;
    }
    private final Object inSavingProperties = new Object();

    public void saveProperties() {
        synchronized (inSavingProperties) {
            FileObject propertiesFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties");
            if (propertiesFile != null) {
                Properties properties = null;
                FileLock lock = null;
                try {
                    lock = propertiesFile.lock();

                    properties = new Properties();

                    LookFeel laf = LookFeel.getCurrent();

                    String lafStr = laf.getClass().getName();
                    String colorReversedStr = Boolean.toString(LookFeel.getCurrent().isPositiveNegativeColorReversed());
                    String thinVolumeStr = Boolean.toString(LookFeel.getCurrent().isThinVolumeBar());
                    String quoteChartTypeStr = LookFeel.getCurrent().getQuoteChartType().toString();
                    String antiAliasStr = Boolean.toString(LookFeel.getCurrent().isAntiAlias());
                    String autoHideScrollStr = Boolean.toString(LookFeel.getCurrent().isAutoHideScroll());

                    String proxyTypeStr = "";
                    String proxyHostStr = "";
                    String proxyPortStr = "";
                    Proxy proxy = UserOptionsManager.getProxy();
                    if (proxy == null) {
                        proxyTypeStr = "SYSTEM";
                    } else {
                        Proxy.Type proxyType = proxy.type();
                        switch (proxyType) {
                            case DIRECT:
                                proxyTypeStr = proxyType.toString();
                                proxyHostStr = "";
                                proxyPortStr = "";
                                break;
                            case HTTP:
                                proxyTypeStr = proxyType.toString();
                                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                                proxyHostStr = addr.getHostName();
                                proxyPortStr = String.valueOf(addr.getPort());
                                break;
                            default:
                                proxyTypeStr = "SYSTEM";
                        }
                    }

                    String strDbDriver = "";
                    String strDbUrl = "";
                    String strDbUser = "";
                    String strDbPassword = "";
                    if (userOptionsProp != null) {
                        strDbDriver = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.driver");
                        strDbUrl = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.url");
                        strDbUser = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.user");
                        strDbPassword = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.password");
                    }

                    properties.setProperty("org.aiotrade.platform.option.lookfeel", lafStr);
                    properties.setProperty("org.aiotrade.platform.option.colorreversed", colorReversedStr);
                    properties.setProperty("org.aiotrade.platform.option.thinvolume", thinVolumeStr);
                    properties.setProperty("org.aiotrade.platform.option.quotecharttype", quoteChartTypeStr);
                    properties.setProperty("org.aiotrade.platform.option.antialias", antiAliasStr);
                    properties.setProperty("org.aiotrade.platform.option.autohidescroll", autoHideScrollStr);

                    properties.setProperty("org.aiotrade.platform.option.proxytype", proxyTypeStr);
                    properties.setProperty("org.aiotrade.platform.option.proxyhost", proxyHostStr);
                    properties.setProperty("org.aiotrade.platform.option.proxyport", proxyPortStr);

                    properties.setProperty("org.aiotrade.platform.jdbc.driver", strDbDriver);
                    properties.setProperty("org.aiotrade.platform.jdbc.url", strDbUrl);
                    properties.setProperty("org.aiotrade.platform.jdbc.user", strDbUser);
                    properties.setProperty("org.aiotrade.platform.jdbc.password", strDbPassword);

                    /** save to file */
                    OutputStream out = propertiesFile.getOutputStream(lock);
                    properties.store(out, null);

                    out.close();
                } catch (IOException ioe) {
                    ErrorManager.getDefault().notify(ioe);
                } finally {
                    if (lock != null) {
                        lock.releaseLock();
                    }
                }

            }
        }

    }

    public void restoreProperties() {
        FileObject propertiesFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties");
        if (propertiesFile != null) {
            userOptionsProp = null;
            try {
                userOptionsProp = new Properties();
                InputStream is = propertiesFile.getInputStream();
                userOptionsProp.load(is);

                is.close();
            } catch (IOException ioe) {
                ErrorManager.getDefault().notify(ioe);
            }

            if (userOptionsProp != null) {
                String lafStr = userOptionsProp.getProperty("org.aiotrade.platform.option.lookfeel");
                String colorReversedStr = userOptionsProp.getProperty("org.aiotrade.platform.option.colorreversed");
                String thinVolumeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.thinvolume");
                String quoteChartTypeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.quotecharttype");
                String antiAliasStr = userOptionsProp.getProperty("org.aiotrade.platform.option.antialias");
                String autoHideScrollStr = userOptionsProp.getProperty("org.aiotrade.platform.option.autohidescroll");
                String proxyTypeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxytype");
                String proxyHostStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxyhost");
                String proxyPortStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxyport", "80");

                if (lafStr != null) {
                    try {
                        LookFeel laf = (LookFeel) Class.forName(lafStr.trim()).newInstance();
                        LookFeel.setCurrent(laf);
                    } catch (Exception ex) {
                        ErrorManager.getDefault().notify(ex);
                    }

                }

                if (colorReversedStr != null) {
                    LookFeel.getCurrent().setPositiveNegativeColorReversed(
                            Boolean.parseBoolean(colorReversedStr.trim()));
                }

                if (thinVolumeStr != null) {
                    LookFeel.getCurrent().setThinVolumeBar(
                            Boolean.parseBoolean(thinVolumeStr.trim()));
                }

                if (quoteChartTypeStr != null) {
                    quoteChartTypeStr = quoteChartTypeStr.trim();
                    if (quoteChartTypeStr.equalsIgnoreCase("bar")) {
                        LookFeel.getCurrent().setQuoteChartType(QuoteChart.Type.Ohlc);
                    } else if (quoteChartTypeStr.equalsIgnoreCase("candle")) {
                        LookFeel.getCurrent().setQuoteChartType(QuoteChart.Type.Candle);
                    } else if (quoteChartTypeStr.equalsIgnoreCase("line")) {
                        LookFeel.getCurrent().setQuoteChartType(QuoteChart.Type.Line);
                    }
                }

                if (antiAliasStr != null) {
                    LookFeel.getCurrent().setAntiAlias(Boolean.parseBoolean(antiAliasStr.trim()));
                }

                /** there may be too many hidden exceptions in the following code, try {} it */
                try {
                    proxyHostStr = (proxyHostStr == null) ? "" : proxyHostStr;
                    int port = Integer.parseInt(proxyPortStr.trim());
                    InetSocketAddress proxyAddr = new InetSocketAddress(proxyHostStr, port);

                    Proxy proxy = null;
                    if (proxyTypeStr != null) {
                        if (proxyTypeStr.equalsIgnoreCase("SYSTEM")) {
                            proxy = null;
                        } else {
                            Proxy.Type type = Proxy.Type.valueOf(proxyTypeStr);
                            switch (type) {
                                case DIRECT:
                                    proxy = Proxy.NO_PROXY;
                                    break;
                                case HTTP:
                                    proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
                                    break;
                                default:
                                    proxy = null;
                            }
                        }
                    }

                    UserOptionsManager.setProxy(proxy);
                } catch (Exception ex) {
                }

                UserOptionsManager.setOptionsLoaded(true);
            }
        }
    }

    public <T extends Comparable> Collection<T> lookupAllRegisteredServices(Class<T> clazz, String folderName) {
        SortedSet<T> result = new TreeSet<T>();

        Lookup lookup = Lookups.forPath(folderName);
        Lookup.Template tp = new Lookup.Template(clazz);
        result.addAll(lookup.lookup(tp).allInstances());

        return result;
    }

    private void checkAndCreateDatabaseIfNecessary() {
        String strDbDriver = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.driver");

        try {
            Class.forName(strDbDriver);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        String strUserDir = System.getProperty("netbeans.user");
        dbUrl = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.url") + strUserDir + "/db/" + "aiotrade";

        String strDbUser = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.user");
        String strDbPassword = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.password");

        dbProp = new Properties();
        dbProp.put("user", strDbUser);
        dbProp.put("password", strDbPassword);

        /** test if database exists, if not, create it: */
        /** derby special properties */
        dbProp.put("create", "true");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbProp);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            if (conn != null && !conn.isClosed()) {
                /** check and create symbol index table if necessary */
                if (!symbolIndexTableExists(conn)) {
                    createSymbolIndexTable(conn);
                }

                conn.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        /** derby special properties */
        dbProp.remove("create");
    }

    private Connection getDbConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbProp);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        /**
         * Try to set Transaction Isolation to TRANSACTION_READ_UNCOMMITTED
         * level to get better perfomance.
         */
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        } catch (SQLException ex) {
            /**
             * As not all databases support TRANSACTION_READ_UNCOMMITTED level,
             * we should catch exception and ignore it here to avoid break the
             * followed actions.
             */
        }

        try {
            conn.setAutoCommit(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return conn;
    }

    /**
     * @param conn a db connection
     * @return true if exists, false if none
     */
    private boolean symbolIndexTableExists(Connection conn) {
        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();

                String existsTestStr = new StringBuilder(100).append("SELECT * FROM ").append(SYMBOL_INDEX_TABLE_NAME).append(" WHERE qsymbol = '").append(TABLE_EXISTS_MARK).append("'").toString();
                try {
                    ResultSet rs = stmt.executeQuery(existsTestStr);
                    if (rs.next()) {
                        return true;
                    }
                } catch (SQLException ex) {
                    /** may be caused by not exist, so don't need to report */
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    private void createSymbolIndexTable(Connection conn) {
        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();

                String stmtCreatTableStr_derby = new StringBuilder(200).append("CREATE TABLE ").append(SYMBOL_INDEX_TABLE_NAME).append(" (").append("qid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, ").append("qsymbol CHAR(30) not null, ").append("qtablename CHAR(60), ").append("qfreq CHAR(10)").append(")").toString();

                String stmtCreatTableStr_h2_hsqldb = new StringBuilder(200).append("CREATE CACHED TABLE ").append(SYMBOL_INDEX_TABLE_NAME).append(" (").append("qid INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, ").append("qsymbol CHAR(30) not null, ").append("qtablename CHAR(60), ").append("qfreq CHAR(10)").append(")").toString();

                String stmtStr = stmtCreatTableStr_h2_hsqldb;
                stmt.executeUpdate(stmtStr);

                /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
                stmtStr = new StringBuilder(100).append("CREATE INDEX idx_").append(SYMBOL_INDEX_TABLE_NAME).append("_qsymbol ON ").append(SYMBOL_INDEX_TABLE_NAME).append(" (qsymbol)").toString();
                stmt.executeUpdate(stmtStr);

                stmtStr = new StringBuilder(100).append("CREATE INDEX idx_").append(SYMBOL_INDEX_TABLE_NAME).append("_qfreq ON ").append(SYMBOL_INDEX_TABLE_NAME).append(" (qfreq)").toString();
                stmt.executeUpdate(stmtStr);

                /** insert a mark record for testing if table exists further */
                stmtStr = new StringBuilder(100).append("INSERT INTO ").append(SYMBOL_INDEX_TABLE_NAME).append(" (qsymbol) VALUES (").append("'").append(TABLE_EXISTS_MARK).append("'").append(")").toString();
                stmt.executeUpdate(stmtStr);

                stmt.close();
                conn.commit();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @param symbol
     * @param freq
     * @param tableName table name
     * @return a connection for following usage if true, null if false
     */
    private Connection tableExists(String symbol, Frequency freq) {
        Connection conn = getDbConnection();
        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();

                String tableName = propTableName(symbol, freq);
                String existsTestStr = new StringBuilder(100).append("SELECT * FROM ").append(tableName).append(" WHERE qtime = ").append(TABLE_EXISTS_MARK).toString();
                try {
                    ResultSet rs = stmt.executeQuery(existsTestStr);
                    if (rs.next()) {
                        return conn;
                    }
                } catch (SQLException ex) {
                    /** may be caused by none exist, so don't need to report */
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    /**
     * @return connection for following usage
     */
    private Connection createTable(String symbol, Frequency freq) {
        Connection conn = getDbConnection();
        if (conn != null) {
            try {
                Statement stmt = conn.createStatement();

                /**
                 * Only one identity column is allowed in each table. Identity
                 * columns are autoincrement columns. They must be of INTEGER or
                 * BIGINT type and are automatically primary key columns (as a
                 * result, multi-column primary keys are not possible with an
                 * IDENTITY column present)
                 */
                String tableName = propTableName(symbol, freq);
                String stmtCreatTableStr_derby = new StringBuilder(200).append("CREATE TABLE ").append(tableName).append(" (").append("qid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, ").append("qtime BIGINT not null, ").append("qopen FLOAT, ").append("qhigh FLOAT, ").append("qlow FLOAT, ").append("qclose FLOAT, ").append("qclose_adj FLOAT, ").append("qvolume FLOAT, ").append("qamount FLOAT, ").append("qwap FLOAT, ").append("qhasgaps SMALLINT, ").append("qsourceid BIGINT").append(")").toString();

                String stmtCreatTableStr_h2_hsqldb = new StringBuilder(200).append("CREATE CACHED TABLE ").append(tableName).append(" (").append("qid INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, ") // IDENTITY(startInt, incrementInt)
                        .append("qtime BIGINT not null, ").append("qopen FLOAT, ").append("qhigh FLOAT, ").append("qlow FLOAT, ").append("qclose FLOAT, ").append("qclose_adj FLOAT, ").append("qvolume FLOAT, ").append("qamount FLOAT, ").append("qwap FLOAT, ").append("qhasgaps SMALLINT, ").append("qsourceid BIGINT").append(")").toString();

                String stmtStr = stmtCreatTableStr_h2_hsqldb;
                stmt.executeUpdate(stmtStr);

                /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
                stmtStr = new StringBuilder(100).append("CREATE INDEX idx_").append(tableName).append("_qtime ON ").append(tableName).append(" (qtime)").toString();
                stmt.executeUpdate(stmtStr);

                stmtStr = new StringBuilder(100).append("CREATE INDEX idx_").append(tableName).append("_qsourceid ON ").append(tableName).append(" (qsourceid)").toString();
                stmt.executeUpdate(stmtStr);

                /** insert a mark record for testing if table exists further */
                stmtStr = new StringBuilder(100).append("INSERT INTO ").append(tableName).append(" (qtime) VALUES (").append(TABLE_EXISTS_MARK).append(")").toString();
                stmt.executeUpdate(stmtStr);

                /** insert a symbol index record into symbol index table */
                stmtStr = new StringBuilder(100).append("INSERT INTO ").append(SYMBOL_INDEX_TABLE_NAME).append(" (qsymbol, qtablename, qfreq) VALUES (").append("'").append(propSymbol(symbol)).append("', '").append(tableName).append("', '").append(freq.toString()).append("')").toString();
                stmt.executeUpdate(stmtStr);

                stmt.close();
                conn.commit();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return conn;
    }

    public void saveQuotes(String symbol, Frequency freq, List<Quote> quotes, long sourceId) {
        Connection conn = tableExists(symbol, freq);
        if (conn == null) {
            conn = createTable(symbol, freq);
            if (conn == null) {
                return;
            }
        }

        try {
            String tableName = propTableName(symbol, freq);
            String stmtStr = new StringBuilder(200).append("INSERT INTO ").append(tableName).append(" (qtime, qopen, qhigh, qlow, qclose, qvolume, qamount, qclose_adj, qwap, qhasgaps, qsourceid)").append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

            PreparedStatement stmt = conn.prepareStatement(stmtStr);
            for (Quote quote : quotes) {
                stmt.setLong(1, quote.getTime());
                stmt.setFloat(2, quote.getOpen());
                stmt.setFloat(3, quote.getHigh());
                stmt.setFloat(4, quote.getLow());
                stmt.setFloat(5, quote.getClose());
                stmt.setFloat(6, quote.getVolume());
                stmt.setFloat(7, quote.getAmount());
                stmt.setFloat(8, quote.getClose_adj());
                stmt.setFloat(9, quote.getWAP());
                stmt.setByte(10, quote.hasGaps() ? (byte) -1 : (byte) 1);
                stmt.setLong(11, sourceId);

                stmt.addBatch();
            }
            stmt.executeBatch();

            stmt.close();
            conn.commit();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Quote> restoreQuotes(String symbol, Frequency freq) {
        List<Quote> quotes = new ArrayList();

        Connection conn = tableExists(symbol, freq);
        if (conn != null) {
            try {
                String tableName = propTableName(symbol, freq);
                String strStmt = new StringBuilder(100).append("SELECT * FROM ").append(tableName).append(" WHERE qtime != ").append(TABLE_EXISTS_MARK).append(" ORDER BY qtime ASC").toString();

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(strStmt);
                while (rs.next()) {
                    Quote quote = getQuotePool().borrowObject();

                    quote.setTime(rs.getLong("qtime"));
                    quote.setOpen(rs.getFloat("qopen"));
                    quote.setHigh(rs.getFloat("qhigh"));
                    quote.setLow(rs.getFloat("qlow"));
                    quote.setClose(rs.getFloat("qclose"));
                    quote.setVolume(rs.getFloat("qvolume"));
                    quote.setAmount(rs.getFloat("qamount"));
                    quote.setClose_adj(rs.getFloat("qclose_adj"));
                    quote.setWAP(rs.getFloat("qwap"));
                    quote.setHasGaps(rs.getByte("qhasgaps") < 0 ? true : false);
                    quote.setSourceId(rs.getLong("qsourceid"));

                    quotes.add(quote);
                }
                rs.close();

                stmt.close();
                conn.commit();
                conn.close();

                WIN_MAN.setStatusText(quotes.size() + " quotes restored from database.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return quotes;
    }

    public void deleteQuotes(String symbol, Frequency freq, long fromTime, long toTime) {
        Connection conn = tableExists(symbol, freq);
        if (conn != null) {
            try {
                String tableName = propTableName(symbol, freq);
                String strStmt = new StringBuilder(100).append("DELETE FROM ").append(tableName).append(" WHERE qtime != ").append(TABLE_EXISTS_MARK).append(" AND qtime BETWEEN ? AND ? ").toString();

                PreparedStatement stmt = conn.prepareStatement(strStmt);

                stmt.setLong(1, fromTime);
                stmt.setLong(2, toTime);

                stmt.execute();

                stmt.close();
                conn.commit();
                conn.close();

                WIN_MAN.setStatusText("Delete data of " + tableName + " successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void dropAllQuoteTables(String symbol) {
        Connection conn = getDbConnection();
        if (symbolIndexTableExists(conn)) {
            try {
                List<String> tableNames = new ArrayList<String>();

                String strStmt = new StringBuilder(100).append("SELECT * FROM ").append(SYMBOL_INDEX_TABLE_NAME).append(" WHERE qsymbol = '").append(propSymbol(symbol)).append("'").toString();

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(strStmt);
                while (rs.next()) {
                    String tableName = rs.getString("qtablename");
                    tableNames.add(tableName);
                }
                rs.close();

                stmt.close();
                conn.commit();

                for (String tableName : tableNames) {
                    strStmt = new StringBuilder(100).append("DROP TABLE ").append(tableName).toString();
                    PreparedStatement dropStmt = conn.prepareStatement(strStmt);
                    dropStmt.execute();

                    dropStmt.close();
                    conn.commit();
                }

                strStmt = new StringBuilder(100).append("DELETE FROM ").append(SYMBOL_INDEX_TABLE_NAME).append(" WHERE qsymbol = '").append(propSymbol(symbol)).append("'").toString();
                PreparedStatement deleteStmt = conn.prepareStatement(strStmt);
                deleteStmt.execute();

                deleteStmt.close();
                conn.commit();

                conn.close();

                WIN_MAN.setStatusText("Clear data of " + symbol + " successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void shutdown() {
        /**
         * Derby special action:
         *
         * In embedded mode, an application should shut down Derby.
         * If the application fails to shut down Derby explicitly,
         * the Derby does not perform a checkpoint when the JVM shuts down,
         * which means that the next connection will be slower.
         * Explicitly shutting down Derby with the URL is preferred.
         * This style of shutdown will always throw an "exception".
         *
         * --------------------------------------------------------
         *
         * For h2 or hsqldb and many other databases:
         *
         * By default, a database is closed when the last connection is closed.
         * However, if it is never closed, the database is closed when the
         * virtual machine exists normally.
         *
         */
        boolean SQLExGot = false;
        try {
            dbProp.put("shutdown", "true");
            Connection conn = DriverManager.getConnection(dbUrl, dbProp);
        } catch (SQLException ex) {
            SQLExGot = true;
        }

        if (SQLExGot == true) {
            /** shutdown sucessfully */
        }

    }

    private final String propTableName(String symbol, Frequency freq) {
        String propSymbol = propSymbol(symbol);
        return new StringBuilder(20).append(propSymbol).append("_").append(freq.getName()).toString();
    }

    /**
     * table name can not contain '.', '^', '-' etc, and should start with letter instead of number
     */
    private final String propSymbol(String symbol) {
        String propSymbol = symbol.trim().replace('^', '_').replace('.', '_').replace('-', '_');
        return new StringBuilder(20).append("q").append(propSymbol).toString();
    }
}
