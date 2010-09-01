package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.collection.JavaConversions._



object GeneralInfos extends Table[GeneralInfo]{

  val publishTime = "publishTime" BIGINT
  val title = "title" VARCHAR(80) DEFAULT("''")
  val url = "url" VARCHAR(100)  DEFAULT("''")
  val infoClass = "infoClass" TINYINT
  val combinValue = "combineValue" BIGINT //for excluding repeated news

  def infoCategorys = inverse(InfoContentCategories.generalInfo)
  def infoSecs = inverse(InfoSecs.generalInfo)
  def infoContents = inverse(Contents.generalInfo)
  def infoAbstracts = inverse(ContentAbstracts.generalInfo)
  
  private val urlToInfo   = new HashMap[String, GeneralInfo]()
  private var isLoad = false

  def infoOf(url : String) : Option[GeneralInfo]= {
    synchronized {
      if(!isLoad){
        val allInfo = (select (GeneralInfos.*) from GeneralInfos list)
        allInfo foreach {
          case x => urlToInfo.put(x.url, x)
        }

      }
      urlToInfo.get(url)
    }
    
  }

  INDEX(getClass.getSimpleName + "_time_idx", publishTime.name)
}

class GeneralInfo extends TVal with Flag with InfoContent {
  var publishTime: Long = _
  var title: String = ""
  var infoClass : Int = _
  var url : String = ""
  var combinValue : Long = _
  
  def infoCategorys : Seq[InfoContentCategory] = GeneralInfos.infoCategorys(this)
  def infoSecs : Seq[InfoSec] = GeneralInfos.infoSecs(this)
  def infoContents : Seq[Content] = GeneralInfos.infoContents(this)
  def infoAbstracts : Seq[ContentAbstract] = GeneralInfos.infoAbstracts(this)


  def infoCategorys_= (v : Seq[InfoContentCategory]) {}
  def infoSecs_= (v : Seq[InfoSec]) {}
  def infoContents_= (v : Seq[Content]) {}
  def infoAbstracts_= (v : Seq[ContentAbstract]) {}


  def weight: Float = 0F
  def link: String = url

  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    map += ("TITLE" -> title)
    
    if(infoAbstracts != null) map += ("CONTENT" -> infoAbstracts(0).content)
    if(infoCategorys != null) map += ("CATEGORY" -> infoCategorys(0).category.name)
    map += ("PUBLISH_TIME" -> publishTime.toString)
    if(link != null) map += ("LINK" -> link)

    map
  }

  def exportToJavaMap: java.util.Map[String, String] = {
    exportToMap
  }

  object InfoClass
  {
    val NEWS = 1
    val FILING = 2
    val ANALYSIS_REPORT = 3
    val SYS_NOTIFICATION = 4
    val QUOTE_INFO = 5
  }
}

