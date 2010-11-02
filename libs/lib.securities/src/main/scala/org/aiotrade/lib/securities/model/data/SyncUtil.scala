/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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

package org.aiotrade.lib.securities.model.data

import java.io.FileOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.info.model.AnalysisReports
import org.aiotrade.lib.info.model.ContentCategories
import org.aiotrade.lib.info.model.GeneralInfos
import org.aiotrade.lib.info.model.InfoContentCategories
import org.aiotrade.lib.info.model.InfoSecs
import org.aiotrade.lib.info.model.ContentAbstracts
import org.aiotrade.lib.info.model.Contents
import org.aiotrade.lib.info.model.Filings
import org.aiotrade.lib.info.model.Newses
import org.aiotrade.lib.sector.model.PortfolioBreakouts
import org.aiotrade.lib.sector.model.Sectors
import org.aiotrade.lib.sector.model.BullVSBears
import org.aiotrade.lib.sector.model.Portfolios
import org.aiotrade.lib.securities.model.Companies
import org.aiotrade.lib.securities.model.CompanyIndustries
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.ExchangeCloseDates
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.Industries
import org.aiotrade.lib.securities.model.MoneyFlows1d
import org.aiotrade.lib.securities.model.MoneyFlows1m
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.SecIssues
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.SecDividends
import org.aiotrade.lib.securities.model.SecInfos
import org.aiotrade.lib.securities.model.SecStatuses
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.TickersLast
import ru.circumflex.orm._
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.QueryDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder
import org.dbunit.operation.DatabaseOperation

/**
 *
 * @author Caoyuan Deng
 */
object SyncUtil {
  private val log = Logger.getLogger(this.getClass.getName)

  private val config = org.aiotrade.lib.util.config.Config()

  private lazy val classLoader = Thread.currentThread.getContextClassLoader

  private var prefixPath = "src/main/resources/"
  private val dataFileDir = prefixPath + "data"
  private val dataFileName = "aiotrade.xml"

  def main(args: Array[String]) {
    Class.forName("com.mysql.jdbc.Driver")
    val fromConn = DriverManager.getConnection("jdbc:mysql://192.168.132.220:3306/faster?useUnicode=true", "root", "") // dburl, user, passwd

    val tableNames = List("companies",
                          "company_industries",
                          "industries",
                          "exchanges",
                          "exchange_close_dates",
                          "secs", "sec_dividends",
                          "sec_infos",
                          "sec_issues",
                          "sec_statuses")
    
    exportToXml(fromConn, "faster", tableNames)
    fromConn.close
  }

  def exportToXml(jdbcConn: Connection, schema: String, tableNames: List[String]) {
    val conn = new DatabaseConnection(jdbcConn, schema)
    val dataSet = new QueryDataSet(conn)
    for (tableName <- tableNames) {
      dataSet.addTable(tableName)
    }
    
    FlatXmlDataSet.write(dataSet, new FileOutputStream(dataFileName))
  }

  /**
   * @Note All tables should be ddl.dropCreate together, since schema will be
   * droped before create tables each time.
   */
  def schema {
    val tables = List(
      // -- basic tables
      Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Companies, CompanyIndustries, Industries,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, TickersLast, Executions,

      // -- info tables
      ContentCategories, GeneralInfos, ContentAbstracts,
      Contents, Newses, Filings, AnalysisReports, InfoSecs, InfoContentCategories,

      // -- sector tables
      BullVSBears, Sectors, Portfolios, PortfolioBreakouts
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => log.info(msg.body))
  }

  def createData {
    schema
    
    val dbDriver = config.getString("orm.connection.driver", "org.h2.driver")
    val dbUrl = config.getString("orm.connection.url", "jdbc:h2:~/.aiotrade/dev/db/aiotrade")
    val dbUsername = config.getString("orm.connection.username", "sa")
    val dbPassword = config.getString("orm.connection.password", "")
    val dbSchema = config.getString("orm.defaultSchema", "orm")
    
    Class.forName(dbDriver)

    val jdbcConn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)
    importToDb(jdbcConn, dbSchema)
  }

  private def importToDb(jdbcConn: Connection, schema: String) {
    val conn = new DatabaseConnection(jdbcConn, schema)
    val dataStream = classLoader.getResourceAsStream("data/" + dataFileName)
    val dataSet = (new FlatXmlDataSetBuilder).build(dataStream)

    try {
      DatabaseOperation.CLEAN_INSERT.execute(conn, dataSet)
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    } finally {
      conn.close
    }
  }
}
