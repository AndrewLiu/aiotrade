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
  def infoAbstract = inverse(ContentAbstracts.generalInfo)
  
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
  
  var infoCategorys : List[ContentCategory] = _
  var infoSecs : List[Sec] = _
  var infoContents : List[Content] = _
  var infoAbstract : List[ContentAbstract] = _

  def weight: Float = 0F
  def link: String = url

  def exportToMap: Map[String, String] = {
    val map = Map[String, String]()
    map += ("title" -> title)
    
    if(infoAbstract != null) map += ("content" -> infoAbstract(0).content)
    if(infoCategorys != null) map += ("category" -> infoCategorys(0).name)
    map += ("publishTime" -> publishTime.toString)
    if(link != null) map += ("link" -> link)

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
  }
}

