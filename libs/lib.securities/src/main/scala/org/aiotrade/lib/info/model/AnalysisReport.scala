package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

object AnalysisReports extends Table[AnalysisReport]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
}

class AnalysisReport extends TVal with Flag with InfoContent{
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

  def publishTime: Long = this.time
  //def weight: Float = 0F
  def link: String = if(generalInfo != null ) generalInfo.url else ""

  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    if(generalInfo.title != null ) map += ("TITLE" -> generalInfo.title)
    if(generalInfo.infoAbstract != null) map += ("SUMMARY" -> generalInfo.infoAbstract(0).content)
    
    map += ("PUBLISH_TIME" -> publishTime.toString)
    //map += ("weight" -> weight.toString)
    if(link != null) map += ("LINK" -> link)
    if(author != null) map +=("PUBLISHER" -> author)
    if(publisher != null) map += ("SOURCE_NAME" -> publisher)
    
    if(generalInfo.infoSecs != null) map += ("SECURITY_CODE" -> generalInfo.infoSecs(0).uniSymbol)
    if(generalInfo.infoCategorys != null) map += ("SUBJECT" -> generalInfo.infoCategorys(0).name)

    map
  }

  def exportToJavaMap: java.util.Map[String, String] = {
    exportToMap
  }
}