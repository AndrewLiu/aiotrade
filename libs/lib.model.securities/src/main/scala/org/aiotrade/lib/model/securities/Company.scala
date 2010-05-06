package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Company extends Table[Company] with LongIdPK[Company] {
  val sec = longColumn("sec_id").references(Sec)
  val validFrom = longColumn("validfrom")
  val validTo = longColumn("validTo").default("-1")
  val shortName = stringColumn("shortName", 30)
  val fullName = stringColumn("fullName", 30)
  val listDate = longColumn("listDate")
}

class Company extends Record[Company](Company) {
  val id = field(Company.id)
  val sec = manyToOne(Company.sec)
  val validFrom = field(Company.validFrom)
  val validTo = field(Company.validTo)
  val shortName = field(Company.shortName)
  val fullName = field(Company.fullName)
  val listDate = field(Company.listDate)
  val industries = oneToMany(CompanyIndustry.company)
}
