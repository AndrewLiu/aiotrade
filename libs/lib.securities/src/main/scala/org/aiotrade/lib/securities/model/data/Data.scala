package org.aiotrade.lib.securities.model.data

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import org.aiotrade.lib.collection.ArrayList
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
import ru.circumflex.orm._
import scala.actors.Scheduler
import scala.collection.mutable.HashMap

/**
 * Dump table to txt using ',' to separat fields
 * mkdir tmp
 * chmod 777 tmp
 * cd tmp
 * mysqldump -ufaster -pfaster -T ./ --database faster --tables secs sec_infos companies industries company_industries --fields-terminated-by=,
 */
object Data {
  var prefixPath = "src/main/resources/"
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

  private val N   = Exchange("N",  "America/New_York", Array(9, 30, 16, 00))  // New York
  private val SS  = Exchange("SS", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shanghai
  private val SZ  = Exchange("SZ", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shenzhen
  private val L   = Exchange("L",  "UTC", Array(8, 00, 15, 30)) // London

  def main(args: Array[String]) {
    println("Current user workind dir: " + System.getProperties.getProperty("user.dir"))
    createData

    Scheduler.shutdown
    System.exit(0)
  }


  def createData {
    schema
    createExchanges
    createSimpleSecs
    readFromSecInfos(new File(dataFileDir, "sec_infos.txt"))
    readFromCompanies(new File(dataFileDir, "companies.txt"))
    readFromIndustries(new File(dataFileDir, "industries.txt"))
    readFromCompanyIndustries(new File(dataFileDir, "company_industries.txt"))
    commit
  }

  def schema {
    val tables = List(
      Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Companies, CompanyIndustries, Industries,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, Executions
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))
  }

  def createExchanges = {
    exchanges = Array(N, SS, SZ, L)
    exchanges foreach println
    Exchanges.insertBatch(exchanges)

    exchanges foreach {x => assert(Exchanges.idOf(x).isDefined, x + " with none id")}
  }

  def readFromSecInfos(file: File) {
    val reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
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
      }
    }
    Secs.insertBatch_!(secRecords.toArray)
    SecInfos.insertBatch_!(secInfoRecords.toArray)
    Secs.updateBatch_!(secRecords.toArray, Secs.secInfo)
  }

  def readFromCompanies(file: File) {
    val reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
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
      }
    }
    Companies.insertBatch_!(companyRecords.toArray)
    Secs.updateBatch_!(secRecords.toArray, Secs.company)
  }

  def readFromIndustries(file: File) {
    val reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
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
      }
    }
    Industries.insertBatch_!(industryRecords.toArray)
  }

  def readFromCompanyIndustries(file: File) {
    val reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
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
      createSimpleSec(symbol, symbol, Exchange.N)
    }

    for (symbol <- List("BP.L", "VOD.L", "BT-A.L", "BARC.L", "BAY.L", "TSCO.L", "HSBA.L")) {
      createSimpleSec(symbol, symbol, Exchange.L)
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
    //commit
  }
}
