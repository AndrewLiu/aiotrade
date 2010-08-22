package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

object ContentCategories extends Table[ContentCategory]{
  val parent = "parent" REFERENCES (ContentCategories)
  val name = "name" VARCHAR(30)
  private val nametocate   = new HashMap[String, ContentCategory]()
  private var isLoad : Boolean = false

  def cateOf(name : String) : Option[ContentCategory] = {
    if(!isLoad)
      {
        load
        isLoad = true
      }
    nametocate.get(name)
  }

  
  private def load() = {
    val categories = (select (ContentCategories.*) from ContentCategories list)
    categories map { case x => nametocate.put(x.name, x)
    }
    nametocate
  }

}

class ContentCategory {
  var parent : ContentCategory = _
  var name : String = ""
}
