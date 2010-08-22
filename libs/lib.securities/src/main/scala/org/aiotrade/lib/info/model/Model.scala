package org.aiotrade.lib.info.model

import ru.circumflex.orm._
import org.aiotrade.lib.util.config
import org.aiotrade.lib.securities.model.Exchange

object Model {

  def main(args: Array[String]) {
    createSamples
    System.exit(0)
  }
  def createSamples = {
    schema
    testsave
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

    ContentCategories.save(parentfiling)


    val period = new ContentCategory()
    period.parent = parentfiling
    period.name = "定期报告"

    ContentCategories.save(period)

    val filingroot = new ContentCategory()
    filingroot.name = "研究报告"
    ContentCategories.save(filingroot)

    val industryAna = new ContentCategory()
    industryAna.name = "行业研究"
    industryAna.parent = filingroot
    ContentCategories.save(industryAna)

    val info = new GeneralInfo()
    info.title = "研究报告测试"
    info.publishTime = 1;
    info.infoClass = info.InfoClass.ANALYSIS_REPORT

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

    ContentCategories.cateOf("行业研究") match
    {
      case Some(x) =>     val infocate1 = new InfoContentCategory()
        infocate1.generalInfo = info
        infocate1.category = x
        InfoContentCategories.save(infocate1)
      case None => Unit
    }


    val filingdemo = new Filing()
    filingdemo.format = filingdemo.Format.pdf
    filingdemo.generalInfo = info
    filingdemo.publisher = "万科"
    filingdemo.size = 30000
    Filings.save(filingdemo)

  }


}
