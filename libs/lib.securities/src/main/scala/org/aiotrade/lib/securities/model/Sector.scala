package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Sectors extends Table[Sector] {
  val category = "category" VARCHAR(30) DEFAULT("''")
  val level = "level" INTEGER()
  val code = "code" VARCHAR(10) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)
  
  val categoryIdx = getClass.getSimpleName + "_category_idx" INDEX(category.name)
  val levelIdx = getClass.getSimpleName + "_level_idx" INDEX(level.name)
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)
}

class Sector {
  var category: String = ""
  var level: Int = 0
  var code: String = ""
  var name: String = ""
  
  var secs: List[Sec] = Nil
}

