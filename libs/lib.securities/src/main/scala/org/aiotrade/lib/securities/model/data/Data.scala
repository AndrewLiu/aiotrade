package org.aiotrade.lib.securities.model.data

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
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
import org.aiotrade.lib.securities.model.Company
import org.aiotrade.lib.securities.model.CompanyIndustries
import org.aiotrade.lib.securities.model.CompanyIndustry
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.ExchangeCloseDates
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.Industry
import org.aiotrade.lib.securities.model.Industries
import org.aiotrade.lib.securities.model.MoneyFlows1d
import org.aiotrade.lib.securities.model.MoneyFlows1m
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.SecIssues
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.SecDividends
import org.aiotrade.lib.securities.model.SecInfo
import org.aiotrade.lib.securities.model.SecInfos
import org.aiotrade.lib.securities.model.SecStatuses
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.TickersLast
import ru.circumflex.orm._
import scala.actors.Scheduler
import scala.collection.mutable.HashMap

/**
 * Dump table to txt using ',' to separat fields
 * mkdir tmp
 * chmod 777 tmp
 * cd tmp
 * mysqldump --default-character-set=utf8 -ufaster -pfaster -T ./ --database faster --tables secs sec_infos companies industries company_industries --fields-terminated-by=,
 *
 * Don't forget to set (in /etc/my.cnf):
 * [mysqld]
 * character-set-server=utf8
 * collation-server=utf8_general_ci
 * init-connect='SET NAMES utf8'
 * [mysql]
 * default-character-set=utf8
 *
 * and check:
 *   mysql> SHOW VARIABLES LIKE 'character%';
 *   mysql> SHOW VARIABLES LIKE 'collation_%';
 *
 * and under mysql client console:
 *   mysql> SET NAMES utf8;
 *
 * and under mysql command line tools:
 *   --default-character-set=utf8
 *
 * and under mysql jdbc url:
 *   jdbc:mysql://localhost:3306/aiotrade?useUnicode=true
 */
object Data {
  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val classLoader = Thread.currentThread.getContextClassLoader
  private var prefixPath = "src/main/resources/"
  lazy val dataFileDir = prefixPath + "data"

  // holding strong reference of exchange
  var exchanges = Array[Exchange]()

  // holding temporary id for secs, companies, industries etc
  val idToSec = HashMap[String, Sec]()
  val idToSecInfo = HashMap[String, SecInfo]()
  val idToCompany = HashMap[String, Company]()
  val idToIndustry = HashMap[String, Industry]()

  val secRecords = new ArrayList[Sec]
  val secInfoRecords = new ArrayList[SecInfo]
  val companyRecords = new ArrayList[Company]()
  val industryRecords = new ArrayList[Industry]()
  val comIndRecords = new ArrayList[CompanyIndustry]()

  private lazy val N   = Exchange("N",  "America/New_York", Array(9, 30, 16, 00))  // New York
  private lazy val SS  = Exchange("SS", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shanghai
  private lazy val SZ  = Exchange("SZ", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shenzhen
  private lazy val L   = Exchange("L",  "UTC", Array(8, 00, 15, 30)) // London
  private lazy val HK   = Exchange("HK",  "Asia/Shanghai", Array(10, 0, 12, 30, 14,30,16,0)) // HongKong
  private lazy val OQ   = Exchange("OQ",  "America/New_York", Array(9, 30, 16, 00)) // NASDAQ

  def main(args: Array[String]) {
    log.info("Current user workind dir: " + System.getProperty("user.dir"))
    log.info("Table of exchanges exists: " + Exchanges.exists)

    createData

    ((SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos)
      ) list) foreach {x =>
      log.info("secs: id=" + x._1 + ", uniSymbol=" + x._2.uniSymbol + ", name=" + x._2.name)
    }

    //companyRecords map (_.shortName) foreach println

    Scheduler.shutdown
    System.exit(0)
  }


  def createData {
    schema
    schemaInfo
    schemaSector
    createExchanges
    createSimpleSecs
    readFromSecInfos(readerOf("sec_infos.txt"))
    readFromCompanies(readerOf("companies.txt"))
    readFromIndustries(readerOf("industries.txt"))
    readFromCompanyIndustries(readerOf("company_industries.txt"))
    Secs.updateBatch_!(secRecords.toArray, Secs.secInfo, Secs.company)
    commit
  }

  def schema {
    val tables = List(
      Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Companies, CompanyIndustries, Industries,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, TickersLast, Executions
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => log.info(msg.body))
  }

  def schemaSector {
    val tables = List(BullVSBears,Sectors, Portfolios, PortfolioBreakouts)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))

  }

  def schemaInfo {
    val tables = List(ContentCategories,GeneralInfos,ContentAbstracts,
                      Contents,Newses,Filings,AnalysisReports,InfoSecs,InfoContentCategories)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))

  }

  def createExchanges = {
    exchanges = Array(SS, SZ, N, L,HK,OQ)
    Exchanges.insertBatch_!(exchanges)
  
    exchanges foreach {x => assert(Exchanges.idOf(x).isDefined, x + " with none id")}
    exchanges foreach {x => log.info("Exchange: " + x + ", id=" + Exchanges.idOf(x).get)}
  }

  def readerOf(fileName: String) = {
    val is = classLoader.getResourceAsStream("data/" + fileName)
    new BufferedReader(new InputStreamReader(is, "UTF-8"))
  }

  def readFromSecInfos(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, secs_id, validFrom, validTo, uniSymbol, name, totalShare, freeFloat, tradingUnit, upperLimit, lowerLimit) =>
          val sec = new Sec
          val exchange = exchangeOf(uniSymbol)
          sec.exchange = exchange
          secRecords += sec
          idToSec.put(secs_id, sec)

          val secInfo = new SecInfo
          secInfo.validFrom = validFrom.toLong
          secInfo.validTo = validTo.toLong
          secInfo.uniSymbol = uniSymbol
          secInfo.name = name
          secInfo.totalShare = totalShare.toLong
          secInfo.freeFloat = freeFloat.toLong
          secInfo.tradingUnit = tradingUnit.toInt
          secInfoRecords += secInfo
          idToSecInfo.put(id, secInfo)
          secInfo.sec = sec

          sec.secInfo = secInfo
          
        case xs => log.warning("sec_infos data file error at line: " + xs.mkString(","))
      }
    }
    Secs.insertBatch_!(secRecords.toArray)
    SecInfos.insertBatch_!(secInfoRecords.toArray)
  }

  def readFromCompanies(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, secs_id, validFrom, validTo, shortName, fullName, listDate) =>
          val company = new Company
          company.validFrom = validFrom.toLong
          company.validTo = validTo.toLong
          company.shortName = shortName
          company.fullName = fullName
          company.listDate = listDate.toLong
          idToSec.get(secs_id) match {
            case Some(sec) => 
              company.sec = sec
              sec.company = company
            case None =>
          }
          companyRecords += company
          idToCompany.put(id, company)

        case xs => log.warning("companies data file error at line: " + xs.mkString(","))
      }
    }
    Companies.insertBatch_!(companyRecords.toArray)
  }

  def readFromIndustries(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, code, level, name, _*) =>
          val industry = new Industry
          industry.code = code
          industry.level = level.toInt
          industry.name = name
          industryRecords += industry
          idToIndustry.put(id, industry)

        case xs => log.warning("industries data file error at line: " + xs.mkString(","))
      }
    }
    Industries.insertBatch_!(industryRecords.toArray)
  }

  def readFromCompanyIndustries(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, company_id, industry_id) =>
          for (com <- idToCompany.get(company_id);
               ind <- idToIndustry.get(industry_id)
          ) {
            val com_ind = new CompanyIndustry
            com_ind.company = com
            com_ind.industry = ind
            comIndRecords += com_ind
          }

        case xs => log.warning("company_industries data file error at line: " + xs.mkString(","))
      }
    }
    CompanyIndustries.insertBatch_!(comIndRecords.toArray)
  }

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) => N
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case _ => SZ
    }
  }

  def createSimpleSecs = {
    val secInfosFile = new File(dataFileDir, "sec_infos.txt")

    for (symbol <- List("GOOG", "YHOO", "ORCL")) {
      createSimpleSec(symbol, symbol, N)
    }

    for (symbol <- List("BP.L", "VOD.L", "BT-A.L", "BARC.L", "BAY.L", "TSCO.L", "HSBA.L")) {
      createSimpleSec(symbol, symbol, L)
    }
  }

  def createSimpleSec(uniSymbol: String, name: String, exchange: Exchange) {
    val secInfo = new SecInfo
    secInfo.uniSymbol = uniSymbol
    secInfo.name = name
    SecInfos.save(secInfo)
    assert(SecInfos.idOf(secInfo).isDefined, secInfo + " with none id")

    val sec = new Sec
    sec.secInfo = secInfo
    sec.exchange = exchange
    Secs.save_!(sec)
    assert(Secs.idOf(sec).isDefined, sec + " with none id")

    secInfo.sec = sec
    SecInfos.update(secInfo)
  }
}
