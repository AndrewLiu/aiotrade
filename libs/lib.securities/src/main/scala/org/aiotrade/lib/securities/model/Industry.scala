package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Industries extends Table[Industry] {
  val category = "category" VARCHAR(30) DEFAULT("''") //field(Industry.category)
  val code = "code" VARCHAR(10) DEFAULT("''") //field(Industry.code)
  val name = "name" VARCHAR(30) DEFAULT("''") //field(Industry.name)

  def companies = inverse(CompanyIndustries.industry) // oneToMany(CompanyIndustry.industry)
}

class Industry {
  var category: String = ""
  var code: String = ""
  var name: String = ""

  var companies: List[Company] = Nil
}
