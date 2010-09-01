package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

object ContentCategories extends Table[ContentCategory]{
  val parent = "parent" BIGINT
  val name = "name" VARCHAR(30)
  val code = "code" VARCHAR(30)
  
  private val codetocate   = new HashMap[String, ContentCategory]()
  private var isLoad : Boolean = false

  def cateOf(code : String) : Option[ContentCategory] = {
    synchronized {
      if(!isLoad)
      {
        load
        isLoad = true
      }
      codetocate.get(code)
      
    }
  }

  
  private def load() = {
    val categories = (select (ContentCategories.*) from ContentCategories list)
    categories map { case x => codetocate.put(x.code, x)
    }
    codetocate
  }

}

class ContentCategory {
  var parent : Long = _
  var name : String = ""
  var code : String = ""
}
