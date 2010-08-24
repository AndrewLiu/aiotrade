package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList

object AnalysisReports extends Table[AnalysisReport]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
}

class AnalysisReport extends TVal with Flag {
  var generalInfo : GeneralInfo = _
  
  var author : String = ""
  var publisher : String = ""

  private var _analysisReports: ArrayList[AnalysisReport] = ArrayList[AnalysisReport]()

  def analysisReports = _analysisReports

  def += [AnalysisReport](value: org.aiotrade.lib.info.model.AnalysisReport){
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _analysisReports
  }

  def ++= [AnalysisReport](values: ArrayList[org.aiotrade.lib.info.model.AnalysisReport]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _analysisReports
  }

}