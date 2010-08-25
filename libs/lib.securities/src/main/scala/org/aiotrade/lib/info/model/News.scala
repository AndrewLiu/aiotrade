package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

object Newses extends Table[News]{

  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val orgPublisher = "orgPublisher" VARCHAR(30) DEFAULT("''")
  val hotness = "hotness" FLOAT()
}

class News extends TVal with Flag with InfoContent{

  var generalInfo : GeneralInfo = _
  var author : String = ""
  var orgPublisher : String = ""
  var hotness : Float = 0F


  private var _newses: ArrayList[News] = ArrayList[News]()

  def newses = _newses

  def += [News](value: org.aiotrade.lib.info.model.News){
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _newses
  }

  def ++= [News](values: ArrayList[org.aiotrade.lib.info.model.News]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _newses
  }

  def publishTime: Long = this.time
  def weight: Float = 0F
  def link: String = if(generalInfo != null ) generalInfo.url else ""
  
  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    if(generalInfo.title != null ) map += ("title" -> generalInfo.title)
    if(generalInfo.infoAbstract != null) map += ("summary" -> generalInfo.infoAbstract(0).content)
    
    map += ("publishTime" -> publishTime.toString)
    if(author != null) map += ("publisher" -> author)
    if(link != null) map += ("link" -> link)
    if(orgPublisher != null) map += ("sourceName" -> orgPublisher)
    map += ("conbineCount" -> hotness.toString)
    map += ("weight" -> weight.toString)

    if(generalInfo.infoSecs != null) map += ("radarName" -> generalInfo.infoSecs(0).uniSymbol)
    if(generalInfo.infoCategorys != null) map += ("subject" -> generalInfo.infoCategorys(0).name)

    map
  }

  def exportToJavaMap: java.util.Map[String, String] = {
    exportToMap
  }
  
  override def toString: String = {
    this.generalInfo.title + "|" + this.generalInfo.publishTime + "|" + this.author
  }
}
