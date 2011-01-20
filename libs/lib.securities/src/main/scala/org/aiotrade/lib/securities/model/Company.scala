package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Companies extends Table[Company] {
  val sec = "secs_id".BIGINT REFERENCES(Secs)

  val validFrom = "validFrom" BIGINT
  val validTo = "validTo" BIGINT //DEFAULT("-1")
  val shortName = "shortName" VARCHAR(30) DEFAULT("''")
  val fullName = "fullName" VARCHAR(100) DEFAULT("''")
  val listDate = "listDate" BIGINT
  
  def industries = inverse(CompanyIndustries.company)
}

class Company {

  var sec: Sec = _

  var validFrom: Long = _
  var validTo: Long = _
  var shortName: String = ""
  var fullName: String = ""
  var listDate: Long = _
  var industries: List[Industry] = Nil
}
