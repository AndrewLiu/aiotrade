package org.aiotrade.lib.info.model

import ru.circumflex.orm._
import org.aiotrade.lib.util.config
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.sector.model._
import org.aiotrade.lib.util.config.Config

object Model {

  def main(args: Array[String]) {
    Config(args(0))
    createSamples
    System.exit(0)
  }
  def createSamples = {
    schema
    testsave
    schemaSector
    testSaveSector
    testSelectSector
    COMMIT
  }

  def schemaSector {
    val tables = List(BullVSBears,Sectors, Portfolios, PortfolioBreakouts)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))
    
  }

  def testSelectSector {
    Sectors.sectorOf("LONG_TERM") match {
      case Some(sector) =>
        /* val breakouts = (SELECT (PortfolioBreakouts.*) FROM PortfolioBreakouts WHERE (PortfolioBreakouts.portfolio.field EQ Portfolios.idOf(sector.portfolio)) list) */
        for(breakout <- sector.portfolio.breakouts){
          println("long-term:" + breakout.sec.secInfo.uniSymbol)
        }
      case None => Unit
    }

    val infos = (SELECT (GeneralInfos.*) FROM GeneralInfos list)
    for(info <- infos) {
      for(category <- info.infoCategorys) {
        println("category:"+category.category.code + ":" + category.category.name)
      }
       for(abstract_ <- info.infoAbstracts) {
         println("abstract:" + abstract_.content)
       }
       for(content <- info.infoContents) {
         println("content:" + content.content)
       }
       for(sec <- info.infoSecs) {
         println("sec:" + sec.sec.secInfo.uniSymbol)
       }
    }
  }

  def testSaveSector {
    val sectorS = new Sector()
    sectorS.code = "SHORT_TERM"
    sectorS.name = "short term portfolio"

    Sectors.save(sectorS)

    val porfolioS = new Portfolio()
    porfolioS.name = "technical porfolio"
    porfolioS.sector = sectorS
    Portfolios.save(porfolioS)

    sectorS.portfolio = porfolioS
    Sectors.update(sectorS)

    Exchange.secOf("600000.SS") match {
      case Some(x) =>
        val breakoutS1 = new PortfolioBreakout()
        breakoutS1.portfolio = porfolioS
        breakoutS1.serialNo = 1
        breakoutS1.rank = 1
        breakoutS1.sec = x
        PortfolioBreakouts.save(breakoutS1)
      case None => Unit

    }

    Exchange.secOf("600001.SS") match {
      case Some(x) =>
        val breakoutS2 = new PortfolioBreakout()
        breakoutS2.portfolio = porfolioS
        breakoutS2.serialNo = 2
        breakoutS2.rank = 1
        breakoutS2.sec = x
        PortfolioBreakouts.save(breakoutS2)
      case None => Unit
    }

    Exchange.secOf("600004.SS") match {
      case Some(x) =>
        val breakoutS3 = new PortfolioBreakout()
        breakoutS3.portfolio = porfolioS
        breakoutS3.serialNo = 3
        breakoutS3.rank = 2
        breakoutS3.sec = x
        PortfolioBreakouts.save(breakoutS3)
      case None => Unit
    }

    val sectorL = new Sector()
    sectorL.code = "LONG_TERM"
    sectorL.name = "long term portfolio"

    Sectors.save(sectorL)

    val porfolioL = new Portfolio()
    porfolioL.name = "fundmental porfolio"
    porfolioL.sector = sectorL
    Portfolios.save(porfolioL)

    sectorL.portfolio = porfolioL
    Sectors.update(sectorL)

    Exchange.secOf("600000.SS") match {
      case  Some(x) =>
        val breakoutL1 = new PortfolioBreakout()
        breakoutL1.portfolio = porfolioL
        breakoutL1.serialNo = 1
        breakoutL1.rank = 1
        breakoutL1.sec =x
        PortfolioBreakouts.save(breakoutL1)
      case None => Unit
    }

    Exchange.secOf("600001.SS") match {
      case Some(x) =>
        val breakoutL2 = new PortfolioBreakout()
        breakoutL2.portfolio = porfolioL
        breakoutL2.serialNo = 2
        breakoutL2.rank = 2
        breakoutL2.sec = x
        PortfolioBreakouts.save(breakoutL2)
      case None => Unit
    }

    Exchange.secOf("000001.SZ") match {
      case Some(x) =>    val breakoutL3 = new PortfolioBreakout()
        breakoutL3.portfolio = porfolioL
        breakoutL3.serialNo = 3
        breakoutL3.rank = 3
        breakoutL3.sec = x
        PortfolioBreakouts.save(breakoutL3)
      case None => Unit
      
    }

    val bullbear = new BullVSBear()
    bullbear.ratio = 0.8f
    bullbear.summary = "good macro economic enviroment"
    bullbear.time = 0L
    BullVSBears.save(bullbear)

  }

  def schema {
    val tables = List(ContentCategories,GeneralInfos,ContentAbstracts,
                      Contents,Newses,Filings,AnalysisReports,InfoSecs,InfoContentCategories)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))

  }

  def testsave {
    val parentfiling = new ContentCategory()
    parentfiling.name = "公告"
    parentfiling.code = "filing"

    ContentCategories.save(parentfiling)


    ContentCategories.idOf(parentfiling) match {
      case Some(id) =>
        val period = new ContentCategory()
        period.parent = id
        period.name = "定期报告"
        period.code = "filing.period"

        ContentCategories.save(period)
      case None => Unit
    }

    val reportroot = new ContentCategory()
    reportroot.name = "研究报告"
    reportroot.code = "report"
    ContentCategories.save(reportroot)

    ContentCategories.idOf(reportroot) match {
      case Some(id) =>
        val industryAna = new ContentCategory()
        industryAna.name = "行业研究"
        industryAna.parent = id
        industryAna.code = "report.industry"
        ContentCategories.save(industryAna)
      case None => Unit
    }




    val info = new GeneralInfo()
    info.title = "研究报告测试"
    info.publishTime = 1;
    info.infoClass = info.InfoClass.ANALYSIS_REPORT
    info.combinValue = 1L

    GeneralInfos.save(info)

    val abstract_ = new ContentAbstract()
    abstract_.content = "this is abstract"
    abstract_.generalInfo = info
    ContentAbstracts.save(abstract_)
    val content_ = new Content()
    content_.content = "This is contenct"
    content_.generalInfo = info
    Contents.save(content_)

    Exchange.secOf("600000.SS") match {
      case Some(x) => val infosec1 = new InfoSec()
        infosec1.sec = x
        infosec1.generalInfo = info
        InfoSecs.save(infosec1)
      case None => Unit
    }

    Exchange.secOf("000001.SZ") match {
      case Some(x) => val infosec2 = new InfoSec()
        infosec2.sec = x
        infosec2.generalInfo = info
        InfoSecs.save(infosec2)
      case None => Unit
    }

    ContentCategories.cateOf("report.industry") match
    {
      case Some(x) =>     val infocate1 = new InfoContentCategory()
        infocate1.generalInfo = info
        infocate1.category = x
        InfoContentCategories.save(infocate1)
      case None => Unit
    }


    val filingdemo = new Filing()
    filingdemo.format = Filing.PDF
    filingdemo.generalInfo = info
    filingdemo.publisher = "万科"
    filingdemo.size = 30000
    Filings.save(filingdemo)

  }


}
