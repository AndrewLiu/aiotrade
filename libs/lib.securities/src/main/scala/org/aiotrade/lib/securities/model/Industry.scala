package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Industries extends Table[Industry] {
  val category = "category" VARCHAR(30) DEFAULT("''")
  val level = "level" INTEGER
  val code = "code" VARCHAR(10) DEFAULT("''")
  val name = "name" VARCHAR(30) DEFAULT("''")

  def companies = inverse(CompanyIndustries.industry)
}

class Industry {
  var category: String = ""
  var level: Int = 0
  var code: String = ""
  var name: String = ""

  var companies: List[Company] = Nil
}
