package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Sec extends Table[Sec] with LongIdPK[Sec] {
  val validFrom = longColumn("validfrom")
  val validTo = longColumn("validTo")
  val valid = booleanColumn("valid")
}

class Sec extends Record[Sec](Sec) {
  val id = field(Sec.id)
  val validFrom = field(Sec.validFrom)
  val validTo = field(Sec.validTo)
  val valid = field(Sec.valid)

  val company = oneToOne(Company.sec)
  val companyHists = oneToMany(Company.sec)

  val secInfo = oneToOne(SecInfo.sec)
  val secInfoHists = oneToMany(SecInfo.sec)
  
  val secIssue = oneToOne(SecIssue.sec)
  val secStatus = oneToMany(SecStatus.sec)
  val secDividends = oneToMany(SecDividend.sec)
  
  val dailyQuotes  = oneToMany(Quote1d.sec)
  val minuteQuotes = oneToMany(Quote1m.sec)

  val dailyMonyFlow  = oneToMany(MoneyFlow1d.sec)
  val minuteMonyFlow = oneToMany(MoneyFlow1m.sec)

  val innerDays = oneToMany(InnerDay.sec)
}