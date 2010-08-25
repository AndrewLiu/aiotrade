package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

object Filings extends Table[Filing]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)
  
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
  val format = "format" TINYINT
  val size = "size" BIGINT
}

class Filing extends TVal with Flag with InfoContent{
  var generalInfo : GeneralInfo = _

  var publisher : String = ""
  var format : Int = _ 
  var size : Long = 0L

  private var _filings: ArrayList[Filing] = ArrayList[Filing]()

  def filings = _filings

  def += [Filings](value: org.aiotrade.lib.info.model.Filing){
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _filings
  }

  def ++= [Filings](values: ArrayList[org.aiotrade.lib.info.model.Filing]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _filings
  }

  def publishTime: Long = this.time
  def weight: Float = 0F
  def link: String = if(generalInfo != null ) generalInfo.url else ""

  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    if(generalInfo.title != null ) map += ("title" -> generalInfo.title)
    map += ("publishTime" -> publishTime.toString)
    map += ("weight" -> weight.toString)
    if(link != null) map += ("link" -> link)
    if(publisher != null) map += ("sourceName" -> publisher)
    map += ("fileSize" -> size.toString)

    if(generalInfo.infoSecs != null) map += ("radarName" -> generalInfo.infoSecs(0).uniSymbol)
    if(generalInfo.infoCategorys != null) map += ("subject" -> generalInfo.infoCategorys(0).name)

    map
  }

  def exportToJavaMap: java.util.Map[String, String] = {
    exportToMap
  }

  object Format {
    val PDF = 1
    val TXT = 2
    val WORD = 3
    val OTHERS = 99
  }
}


