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

package org.aiotrade.lib.securities.data

import java.io.File
import java.io.FileOutputStream
import java.net.JarURLConnection
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

/**
 *
 * @author Caoyuan Deng
 */
object SyncUtil {
  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val classLoader = Thread.currentThread.getContextClassLoader

  private val srcMainResources = "src/main/resources/"
  private val exportDataDir = srcMainResources + "data"
  private val dataFileName = "aiotrade.xml"

  /**
   * @Note lazy call them so we can specify config file before orm package
   */
  private lazy val tables = List(Companies,
                                 CompanyIndustries,
                                 Industries,
                                 Exchanges,
                                 ExchangeCloseDates,
                                 Secs,
                                 SecDividends,
                                 SecInfos,
                                 SecIssues,
                                 SecStatuses
  )

  private val tableNames = List("companies",
                                "company_industries",
                                "industries",
                                "exchanges",
                                "exchange_close_dates",
                                "secs",
                                "sec_dividends",
                                "sec_infos",
                                "sec_issues",
                                "sec_statuses")

  private val mysqlDriver = "com.mysql.jdbc.Driver"

  def main(args: Array[String]) {
    exportAvroDataFileFromProductionMysql
    //createData(exportDataDir)
    //importDataToLocalTestMysql
  }

  def exportAvroDataFileFromProductionMysql {
    org.aiotrade.lib.util.config.Config(srcMainResources + "/export_data.conf")
    exportToAvro(tables)
  }


  // --- API methods

  /**
   * all avro file size is about 1708959
   */
  def exportToAvro(tables: Seq[Table[_]]) {
    val t0 = System.currentTimeMillis
    tables foreach {x => exportToAvro(x)}
    log.info("Exported to avro in " + (System.currentTimeMillis - t0) + " ms.")
  }

  private def exportToAvro[R](x: Relation[R]) {
    SELECT (x.*) FROM (x) toAvro(exportDataDir + "/" + x.relationName + ".avro")
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

  def importDataFrom(dataDir: String) {
    var t0 = System.currentTimeMillis
    schema
    log.info("Created schema in " + (System.currentTimeMillis - t0) / 1000.0 + " s.")
    
    t0 = System.currentTimeMillis
    val records = tables map {x => holdAvroRecords(dataDir + "/" +  x.relationName + ".avro", x)}
    tables foreach {x => importAvroToDb(dataDir + "/" +  x.relationName + ".avro", x)}
    COMMIT
    log.info("Imported data to db in " + (System.currentTimeMillis - t0) / 1000.0 + " s.")
  }
  
  /**
   * Extract data to destPath from jar file
   * @see http://bits.netbeans.org/dev/javadoc/org-openide-modules/org/openide/modules/InstalledFileLocator.html
   */
  def extractDataTo(destPath: String) {
    try {
      // locate jar 
      val c = classOf[Locator]
      // @Note We'll get a org.netbeans.JarClassLoader$NbJarURLConnection, which seems cannot call jarUrl.openStream
      val jarUrl = c.getProtectionDomain.getCodeSource.getLocation
      log.info("Initial data is located at: " + jarUrl)

      val start = System.currentTimeMillis

      val buf = new Array[Byte](1024)
      val jarFile = jarUrl.openConnection.asInstanceOf[JarURLConnection].getJarFile
      val entries = jarFile.entries
      while (entries.hasMoreElements) {
        val entry = entries.nextElement
        val entryName = entry.getName
        if (entryName != "data/" && entryName.startsWith("data/")) {
          var fileName = entryName.substring(4, entryName.length)
          if (fileName.charAt(fileName.length - 1) == '/') fileName = fileName.substring(0, fileName.length - 1)
          if (fileName.charAt(0) == '/') fileName = fileName.substring(1)
          if (File.separatorChar != '/') fileName = fileName.replace('/', File.separatorChar)
        

          val file = new File(destPath, fileName)
          if (entry.isDirectory) {
            // make sure the directory exists
            file.mkdirs
          }  else {
            // make sure the directory exists
            val parent = file.getParentFile
            if (parent != null && !parent.exists) {
              parent.mkdirs
            }
            
            // dump the file
            val in = jarFile.getInputStream(entry)
            val out = new FileOutputStream(file)
            var len = 0
            while ({len = in.read(buf, 0, buf.length); len != -1}) {
              out.write(buf, 0, len)
            }
            out.flush
            out.close
            file.setLastModified(entry.getTime)
            in.close
          }
        }         
      }
      
      log.info("Extract data in " + (System.currentTimeMillis - start) + "ms")
    } catch {
      case e => log.log(Level.WARNING, e.getMessage, e)
    }
  }
    

  private def holdAvroRecords[R](avroFile: String, table: Table[R]) = {
    SELECT (table.*) FROM (AVRO(table, avroFile)) list()
  }

  /**
   * @Note
   * per: tables foreach {x => ...}, 'x' will be infered by Scala as ScalaObject, we have to define
   * this standalone function to get type right
   */
  private def importAvroToDb[R: Manifest](avroFile: String, table: Table[R]) {
    val records = SELECT (table.*) FROM (AVRO(table, avroFile)) list()
    table.insertBatch(records.toArray, false)
  }
  
}
