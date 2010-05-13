package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Company extends Table[Company] {
  val sec = "sec_id" REFERENCES(Sec) //manyToOne(Company.sec)

  val validFrom = "validFrom" BIGINT
  val validTo = "validTo" BIGINT //DEFAULT("-1")
  val shortName = "shortName" VARCHAR(30) DEFAULT("''")
  val fullName = "fullName" VARCHAR(30) DEFAULT("''")
  val listDate = "listDate" BIGINT
  
  def industries = inverse(CompanyIndustry.company) //oneToMany(CompanyIndustry.company)
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
