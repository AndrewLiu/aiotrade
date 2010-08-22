package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable.HashMap


object GeneralInfos extends Table[GeneralInfo]{

  val publishTime = "publishTime" BIGINT
  val title = "title" VARCHAR(80) DEFAULT("''")
  val url = "url" VARCHAR(100)  DEFAULT("''")
  val infoClass = "infoClass" TINYINT

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


class GeneralInfo {
  var publishTime: Long = _
  var title: String = ""
  var infoClass : Int = _
  var url : String = ""
  
  var infoCategorys : List[ContentCategory] = _
  var infoSecs : List[Sec] = _
  var infoContents : List[Content] = _
  var infoAbstract : List[ContentAbstract] = _

  object InfoClass
  {
    val NEWS = 1
    val FILING = 2
    val ANALYSIS_REPORT = 3
    val SYS_NOTIFICATION = 4
  }
}

