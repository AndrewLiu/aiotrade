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
package org.aiotrade.modules.ui.netbeans;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.aiotrade.lib.charting.chart.QuoteChart;
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.view.securities.persistence.ContentsPersistenceHandler
import org.aiotrade.lib.view.securities.persistence.ContentsParseHandler
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.Quote
import org.aiotrade.lib.securities.QuotePool
import org.aiotrade.lib.securities.TickerPool
import org.aiotrade.lib.securities.util.UserOptionsManager
import org.aiotrade.lib.util.swing.action.RefreshAction;
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import scala.collection.mutable.ArrayBuffer


/**
 * This class implements persistence via NetBeans Node system
 * Actually it will be an application context
 *
 * @author Caoyuan Deng
 */
object NetBeansPersistenceManager {
}

class NetBeansPersistenceManager extends PersistenceManager {
  import NetBeansPersistenceManager._

  /** @TODO
   * WindowManager.getDefault()
   */
  private val WIN_MAN = new NetBeansWindowManager
  /**
   * we perfer contents instances long lives in application context, so, don't
   * use weak reference map here.
   */
  val defaultContents: AnalysisContents = restoreContents("Default")
  val quotePool = new QuotePool
  val tickerPool = new TickerPool
  private val TABLE_EXISTS_MARK = Long.MaxValue.toString
  private val SYMBOL_INDEX_TABLE_NAME = "AIOTRADESYMBOLINDEX"
  private var userOptionsProp: Properties = _
  private var dbProp: Properties = _
  private var dbUrl: String = _
  private val inSavingProperties = new Object

  restoreProperties
  checkAndCreateDatabaseIfNecessary

  def saveContents(contents: AnalysisContents) {
    if (contents.uniSymbol.equalsIgnoreCase("Default")) {
      val defaultContentsFile = FileUtil.getConfigFile("UserOptions/DefaultContents.xml")
      if (defaultContentsFile != null) {
        var lock: FileLock = null
        try {
          lock = defaultContentsFile.lock

          val out = new PrintStream(defaultContentsFile.getOutputStream(lock))
          out.print(ContentsPersistenceHandler.dumpContents(contents))

          /** should remember to do out.close() here */
          out.close
        } catch {case ex: IOException => ErrorManager.getDefault.notify(ex)
        } finally {
          if (lock != null) {
            lock.releaseLock
          }
        }
      }
    } else {
      SymbolNodes.occupantNodeOf(contents) foreach {node =>
        /** refresh node's icon in explorer window */
        val children = node.getChildren
        for (child <- children.getNodes) {
          child.getLookup.lookup(classOf[RefreshAction]).execute
        }

        val dob = node.getLookup.lookup(classOf[DataObject])
        val writeTo = dob.getPrimaryFile
        var lock: FileLock = null
        try {
          lock = writeTo.lock

          val out = new PrintStream(writeTo.getOutputStream(lock))
          out.print(ContentsPersistenceHandler.dumpContents(contents))

          /** should remember to do out.close() here */
          out.close
        } catch {case ex: IOException => ErrorManager.getDefault().notify(ex)
        } finally {
          if (lock != null) {
            lock.releaseLock
          }
        }
      }

    }
  }

  def restoreContents(symbol: String): AnalysisContents = {
    var contents: AnalysisContents = null;

    if (symbol.equalsIgnoreCase("Default")) {
      val defaultContentsFile = FileUtil.getConfigFile("UserOptions/DefaultContents.xml");
      if (defaultContentsFile != null) {
        try {
          val is = defaultContentsFile.getInputStream
          val xmlReader = XMLUtil.createXMLReader
          val handler = new ContentsParseHandler
          xmlReader.setContentHandler(handler)
          xmlReader.parse(new InputSource(is))
          contents = handler.getContents

          is.close
        } catch {
          case ex: IOException  => ErrorManager.getDefault.notify(ex)
          case ex: SAXException => ErrorManager.getDefault.notify(ex)
        }
      }
    } else {
      /** @TODO
       *  useful or useless in this case? */
    }

    contents
  }

  def saveProperties {
    inSavingProperties synchronized {
      val propertiesFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties");
      if (propertiesFile != null) {
        var properties: Properties = null;
        var lock: FileLock = null;
        try {
          lock = propertiesFile.lock

          properties = new Properties

          val laf = LookFeel

          val lafStr = laf.getClass().getName
          val colorReversedStr = LookFeel().isPositiveNegativeColorReversed.toString
          val thinVolumeStr = LookFeel().isThinVolumeBar.toString;
          val quoteChartTypeStr = LookFeel().getQuoteChartType.toString
          val antiAliasStr = LookFeel().isAntiAlias.toString
          val autoHideScrollStr = LookFeel().isAutoHideScroll.toString

          var proxyTypeStr = ""
          var proxyHostStr = ""
          var proxyPortStr = ""
          var proxy = UserOptionsManager.getProxy
          if (proxy == null) {
            proxyTypeStr = "SYSTEM"
          } else {
            val proxyType = proxy.`type`
            proxyType match {
              case Proxy.Type.DIRECT =>
                proxyTypeStr = proxyType.toString
                proxyHostStr = ""
                proxyPortStr = ""
              case Proxy.Type.HTTP =>
                proxyTypeStr = proxyType.toString
                val addr = proxy.address.asInstanceOf[InetSocketAddress]
                proxyHostStr = addr.getHostName
                proxyPortStr = addr.getPort.toString
              case _ =>
                proxyTypeStr = "SYSTEM"
            }
          }

          var strDbDriver = ""
          var strDbUrl = ""
          var strDbUser = ""
          var strDbPassword = ""
          if (userOptionsProp != null) {
            strDbDriver = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.driver")
            strDbUrl = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.url")
            strDbUser = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.user")
            strDbPassword = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.password")
          }

          properties.setProperty("org.aiotrade.platform.option.lookfeel", lafStr)
          properties.setProperty("org.aiotrade.platform.option.colorreversed", colorReversedStr)
          properties.setProperty("org.aiotrade.platform.option.thinvolume", thinVolumeStr)
          properties.setProperty("org.aiotrade.platform.option.quotecharttype", quoteChartTypeStr)
          properties.setProperty("org.aiotrade.platform.option.antialias", antiAliasStr)
          properties.setProperty("org.aiotrade.platform.option.autohidescroll", autoHideScrollStr)

          properties.setProperty("org.aiotrade.platform.option.proxytype", proxyTypeStr)
          properties.setProperty("org.aiotrade.platform.option.proxyhost", proxyHostStr)
          properties.setProperty("org.aiotrade.platform.option.proxyport", proxyPortStr)

          properties.setProperty("org.aiotrade.platform.jdbc.driver", strDbDriver)
          properties.setProperty("org.aiotrade.platform.jdbc.url", strDbUrl)
          properties.setProperty("org.aiotrade.platform.jdbc.user", strDbUser)
          properties.setProperty("org.aiotrade.platform.jdbc.password", strDbPassword)

          /** save to file */
          val out = propertiesFile.getOutputStream(lock);
          properties.store(out, null);

          out.close
        } catch {case ex: IOException => ErrorManager.getDefault().notify(ex);
        } finally {
          if (lock != null) {
            lock.releaseLock
          }
        }

      }
    }

  }

  def restoreProperties {
    val propertiesFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties");
    if (propertiesFile != null) {
      userOptionsProp = null
      try {
        userOptionsProp = new Properties
        val is = propertiesFile.getInputStream
        userOptionsProp.load(is)

        is.close
      } catch {case ex: IOException => ErrorManager.getDefault().notify(ex)}

      if (userOptionsProp != null) {
        val lafStr = userOptionsProp.getProperty("org.aiotrade.platform.option.lookfeel")
        val colorReversedStr = userOptionsProp.getProperty("org.aiotrade.platform.option.colorreversed")
        val thinVolumeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.thinvolume")
        var quoteChartTypeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.quotecharttype")
        val antiAliasStr = userOptionsProp.getProperty("org.aiotrade.platform.option.antialias")
        val autoHideScrollStr = userOptionsProp.getProperty("org.aiotrade.platform.option.autohidescroll")
        val proxyTypeStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxytype")
        var proxyHostStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxyhost")
        val proxyPortStr = userOptionsProp.getProperty("org.aiotrade.platform.option.proxyport", "80")

        if (lafStr != null) {
          try {
            val laf = Class.forName(lafStr.trim).newInstance.asInstanceOf[LookFeel]
            LookFeel() = laf
          } catch {case ex: Exception => ErrorManager.getDefault.notify(ex)}

        }

        if (colorReversedStr != null) {
          LookFeel().setPositiveNegativeColorReversed(colorReversedStr.trim.toBoolean)
        }

        if (thinVolumeStr != null) {
          LookFeel().setThinVolumeBar(thinVolumeStr.trim.toBoolean)
        }

        if (quoteChartTypeStr != null) {
          quoteChartTypeStr = quoteChartTypeStr.trim
          if (quoteChartTypeStr.equalsIgnoreCase("bar")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Ohlc);
          } else if (quoteChartTypeStr.equalsIgnoreCase("candle")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Candle);
          } else if (quoteChartTypeStr.equalsIgnoreCase("line")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Line);
          }
        }

        if (antiAliasStr != null) {
          LookFeel().setAntiAlias(antiAliasStr.trim.toBoolean)
        }

        /** there may be too many hidden exceptions in the following code, try {} it */
        try {
          proxyHostStr = if (proxyHostStr == null) "" else proxyHostStr
          val port = proxyPortStr.trim.toInt
          val proxyAddr = new InetSocketAddress(proxyHostStr, port);

          val proxy: Proxy = if (proxyTypeStr != null) {
            if (proxyTypeStr.equalsIgnoreCase("SYSTEM")) {
              null
            } else {
              var tpe = Proxy.Type.valueOf(proxyTypeStr)
              tpe match {
                case Proxy.Type.DIRECT => Proxy.NO_PROXY
                case Proxy.Type.HTTP => new Proxy(Proxy.Type.HTTP, proxyAddr)
                case _ => null
              }
            }
          } else null

          UserOptionsManager.setProxy(proxy)
        } catch {case ex: Exception =>}

        UserOptionsManager.setOptionsLoaded(true)
      }
    }
  }

  def lookupAllRegisteredServices[T](clz: Class[T], folderName: String): Seq[T] = {
    val lookup = Lookups.forPath(folderName)
    val tp = new Lookup.Template(clz)
    val instances = lookup.lookup(tp).allInstances.iterator
    var sinstances = List[T]()
    while (instances.hasNext) sinstances ::= instances.next
    sinstances
  }

  private def checkAndCreateDatabaseIfNecessary {
    val strDbDriver = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.driver")

    try {
      Class.forName(strDbDriver)
    } catch {case ex: ClassNotFoundException => ex.printStackTrace}

    val strUserDir = System.getProperty("netbeans.user")
    dbUrl = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.url") + strUserDir + "/db/" + "aiotrade"

    val strDbUser = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.user")
    val strDbPassword = userOptionsProp.getProperty("org.aiotrade.platform.jdbc.password")

    dbProp = new Properties
    dbProp.put("user", strDbUser)
    dbProp.put("password", strDbPassword)

    /** test if database exists, if not, create it: */
    /** derby special properties */
    dbProp.put("create", "true")

    val conn = try {
      DriverManager.getConnection(dbUrl, dbProp)
    } catch {case ex: SQLException => ex.printStackTrace; null}

    if (conn != null && !conn.isClosed) {
      try {
        /** check and create symbol index table if necessary */
        if (!symbolIndexTableExists(conn)) {
          createSymbolTable(conn)
        }

        conn.close
      } catch {case ex: SQLException => ex.printStackTrace}
    }

    /** derby special properties */
    dbProp.remove("create")
  }

  private def getDbConnection: Connection = {
    val conn = try {
      DriverManager.getConnection(dbUrl, dbProp)
    } catch {case ex: SQLException => ex.printStackTrace; null}

    /**
     * Try to set Transaction Isolation to TRANSACTION_READ_UNCOMMITTED
     * level to get better perfomance.
     */
    try {
      conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    } catch {case ex: SQLException =>
        /**
         * As not all databases support TRANSACTION_READ_UNCOMMITTED level,
         * we should catch exception and ignore it here to avoid break the
         * followed actions.
         */
    }

    try {
      conn.setAutoCommit(false)
    } catch {case ex: SQLException => ex.printStackTrace}

    conn
  }

  /**
   * @param conn a db connection
   * @return true if exists, false if none
   */
  private def symbolIndexTableExists(conn: Connection): Boolean = {
    if (conn != null) {
      try {
        val stmt = conn.createStatement

        val existsTestStr = "SELECT * FROM " + SYMBOL_INDEX_TABLE_NAME + " WHERE qsymbol = '" + TABLE_EXISTS_MARK + "'"
        try {
          val rs = stmt.executeQuery(existsTestStr)
          if (rs.next) {
            return true
          }
        } catch {case ex: SQLException =>
            /** may be caused by not exist, so don't need to report */
        }
      } catch {case ex: SQLException => ex.printStackTrace}
    }

    false
  }

  private def createSymbolTable(conn: Connection) {
    if (conn != null) {
      try {
        val stmt = conn.createStatement

        val stmtCreatTableStr_derby = 
          "CREATE TABLE " + SYMBOL_INDEX_TABLE_NAME + " (" + "qid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, qsymbol CHAR(30) not null, qtablename CHAR(60), qfreq CHAR(10))"

        val stmtCreatTableStr_h2_hsqldb = 
          "CREATE CACHED TABLE " + SYMBOL_INDEX_TABLE_NAME + " (qid INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, qsymbol CHAR(30) not null, qtablename CHAR(60), qfreq CHAR(10))"

        var stmtStr = stmtCreatTableStr_h2_hsqldb
        stmt.executeUpdate(stmtStr)

        /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
        stmtStr = "CREATE INDEX idx_" + SYMBOL_INDEX_TABLE_NAME + "_qsymbol ON " + SYMBOL_INDEX_TABLE_NAME + " (qsymbol)"
        stmt.executeUpdate(stmtStr)

        stmtStr = "CREATE INDEX idx_" + SYMBOL_INDEX_TABLE_NAME + "_qfreq ON " + SYMBOL_INDEX_TABLE_NAME + " (qfreq)"
        stmt.executeUpdate(stmtStr)

        /** insert a mark record for testing if table exists further */
        stmtStr = "INSERT INTO " + SYMBOL_INDEX_TABLE_NAME + " (qsymbol) VALUES ('" + TABLE_EXISTS_MARK + "')"
        stmt.executeUpdate(stmtStr)

        stmt.close
        conn.commit
      } catch {case ex: SQLException => ex.printStackTrace}
    }
  }

  /**
   * @param symbol
   * @param freq
   * @param tableName table name
   * @return a connection for following usage if true, null if false
   */
  private def tableExists(symbol: String, freq: TFreq): Connection = {
    val conn = getDbConnection
    if (conn != null) {
      try {
        val stmt = conn.createStatement

        val tableName = propTableName(symbol, freq)
        val existsTestStr = "SELECT * FROM " + tableName + " WHERE qtime = " + TABLE_EXISTS_MARK
        try {
          val rs = stmt.executeQuery(existsTestStr)
          if (rs.next) {
            return conn
          }
        } catch {case ex: SQLException =>
            /** may be caused by none exist, so don't need to report */
        }
      } catch {case ex: SQLException => ex.printStackTrace}
    }

    return null;
  }

  /**
   * @return connection for following usage
   */
  private def createTable(symbol: String, freq: TFreq): Connection = {
    val conn = getDbConnection
    if (conn != null) {
      try {
        val stmt = conn.createStatement

        /**
         * Only one identity column is allowed in each table. Identity
         * columns are autoincrement columns. They must be of INTEGER or
         * BIGINT type and are automatically primary key columns (as a
         * result, multi-column primary keys are not possible with an
         * IDENTITY column present)
         */
        val tableName = propTableName(symbol, freq)
        val stmtCreatTableStr_derby = "CREATE TABLE " + tableName + " (qid INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
        "qtime BIGINT not null, qopen FLOAT, qhigh FLOAT, qlow FLOAT, qclose FLOAT, qclose_adj FLOAT, qvolume FLOAT, qamount FLOAT, qvwap FLOAT, qhasgaps SMALLINT, qsourceid BIGINT)"

        val stmtCreatTableStr_h2_hsqldb = "CREATE CACHED TABLE " + tableName + " (qid INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, " +
        "qtime BIGINT not null, qopen FLOAT, qhigh FLOAT, qlow FLOAT, qclose FLOAT, qclose_adj FLOAT, qvolume FLOAT, qamount FLOAT, qvwap FLOAT, qhasgaps SMALLINT, qsourceid BIGINT)"

        var stmtStr = stmtCreatTableStr_h2_hsqldb
        stmt.executeUpdate(stmtStr)

        /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
        stmtStr = "CREATE INDEX idx_" + tableName + "_qtime ON " + tableName + " (qtime)"
        stmt.executeUpdate(stmtStr)

        stmtStr = "CREATE INDEX idx_" + tableName + "_qsourceid ON " + tableName + " (qsourceid)"
        stmt.executeUpdate(stmtStr)

        /** insert a mark record for testing if table exists further */
        stmtStr = "INSERT INTO " + tableName + " (qtime) VALUES (" + TABLE_EXISTS_MARK + ")"
        stmt.executeUpdate(stmtStr)

        /** insert a symbol index record into symbol index table */
        stmtStr = "INSERT INTO " + SYMBOL_INDEX_TABLE_NAME + " (qsymbol, qtablename, qfreq) VALUES ('" + propSymbol(symbol) + "', '" + tableName + "', '" + freq.toString + "')"
        stmt.executeUpdate(stmtStr)

        stmt.close
        conn.commit

      } catch {case ex: SQLException => ex.printStackTrace}
    }

    conn
  }

  def saveQuotes(symbol: String, freq: TFreq, quotes: Array[Quote], sourceId: Long) {
    var conn = tableExists(symbol, freq)
    if (conn == null) {
      conn = createTable(symbol, freq)
      if (conn == null) {
        return
      }
    }

    try {
      val tableName = propTableName(symbol, freq)
      val stmtStr =  "INSERT INTO " + tableName  +
      " (qtime, qopen, qhigh, qlow, qclose, qvolume, qamount, qclose_adj, qvwap, qhasgaps, qsourceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

      val stmt = conn.prepareStatement(stmtStr)
      for (quote <- quotes) {
        stmt setLong  (1, quote.time)
        stmt setFloat (2, quote.open)
        stmt setFloat (3, quote.high)
        stmt setFloat (4, quote.low)
        stmt setFloat (5, quote.close)
        stmt setFloat (6, quote.volume)
        stmt setFloat (7, quote.amount)
        stmt setFloat (8, quote.close_adj)
        stmt setFloat (9, quote.vwap)
        stmt setByte  (10, if (quote.hasGaps) -1 else 1)
        stmt setLong  (11, sourceId)

        stmt.addBatch
      }
      stmt.executeBatch

      stmt.close
      conn.commit
      conn.close
    } catch {case ex: SQLException => ex.printStackTrace}
  }

  def restoreQuotes(symbol: String, freq: TFreq): Array[Quote] = {
    val quotes = ArrayBuffer[Quote]()

    val conn = tableExists(symbol, freq)
    if (conn != null) {
      try {
        val tableName = propTableName(symbol, freq)
        val strStmt = "SELECT * FROM " + tableName + " WHERE qtime != " + TABLE_EXISTS_MARK + " ORDER BY qtime ASC"

        val stmt = conn.createStatement
        val rs = stmt.executeQuery(strStmt)
        while (rs.next) {
          val quote = quotePool.borrowObject

          quote.time = rs.getLong("qtime")
          quote.open = rs.getFloat("qopen")
          quote.high = rs.getFloat("qhigh")
          quote.low = rs.getFloat("qlow")
          quote.close = rs.getFloat("qclose")
          quote.volume = rs.getFloat("qvolume")
          quote.amount = rs.getFloat("qamount")
          quote.close_adj = rs.getFloat("qclose_adj")
          quote.vwap = rs.getFloat("qvwap")
          quote.hasGaps = (if (rs.getByte("qhasgaps") < 0) true else false)
          quote.sourceId = rs.getLong("qsourceid")

          quotes += quote
        }
        rs.close

        stmt.close
        conn.commit
        conn.close

        WIN_MAN.setStatusText(quotes.size + " quotes restored from database.")
      } catch {case ex: SQLException => ex.printStackTrace}
    }

    quotes.toArray
  }

  def deleteQuotes(symbol: String, freq: TFreq, fromTime: Long, toTime: Long) {
    val conn = tableExists(symbol, freq)
    if (conn != null) {
      try {
        val tableName = propTableName(symbol, freq)
        val strStmt = "DELETE FROM " + tableName + " WHERE qtime != " + TABLE_EXISTS_MARK + " AND qtime BETWEEN ? AND ? "

        val stmt = conn.prepareStatement(strStmt)

        stmt.setLong(1, fromTime)
        stmt.setLong(2, toTime)

        stmt.execute

        stmt.close
        conn.commit
        conn.close

        WIN_MAN.setStatusText("Delete data of " + tableName + " successfully.")
      } catch {case ex: SQLException => ex.printStackTrace}
    }
  }

  def dropAllQuoteTables(symbol: String) {
    val conn = getDbConnection
    if (symbolIndexTableExists(conn)) {
      try {
        var tableNames = List[String]()

        var strStmt = "SELECT * FROM " + SYMBOL_INDEX_TABLE_NAME + " WHERE qsymbol = '" + propSymbol(symbol) + "'"

        val stmt = conn.createStatement
        val rs = stmt.executeQuery(strStmt)
        while (rs.next) {
          val tableName = rs.getString("qtablename")
          tableNames ::= tableName
        }
        rs.close

        stmt.close
        conn.commit

        for (tableName <- tableNames) {
          strStmt = "DROP TABLE " + tableName
          val dropStmt = conn.prepareStatement(strStmt)
          dropStmt.execute

          dropStmt.close
          conn.commit
        }

        strStmt = "DELETE FROM " + SYMBOL_INDEX_TABLE_NAME + " WHERE qsymbol = '" + propSymbol(symbol) + "'"
        val deleteStmt = conn.prepareStatement(strStmt)
        deleteStmt.execute

        deleteStmt.close
        conn.commit

        conn.close

        WIN_MAN.setStatusText("Clear data of " + symbol + " successfully.")
      } catch {case ex: SQLException =>ex.printStackTrace}
    }
  }

  def shutdown {
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
    var SQLExGot = false
    try {
      dbProp.put("shutdown", "true")
      val conn = DriverManager.getConnection(dbUrl, dbProp)
    } catch {case ex: SQLException => SQLExGot = true}

    if (SQLExGot == true) {
      /** shutdown sucessfully */
    }

  }

  private def propTableName(symbol: String, freq: TFreq): String = {
    val sym = propSymbol(symbol)
    sym + "_" + freq.name
  }

  /**
   * table name can not contain '.', '^', '-' etc, and should start with letter instead of number
   */
  private def propSymbol(symbol: String): String = {
    val propSymbol = symbol.trim.replace('^', '_').replace('.', '_').replace('-', '_')
    "q" + propSymbol
  }
}
