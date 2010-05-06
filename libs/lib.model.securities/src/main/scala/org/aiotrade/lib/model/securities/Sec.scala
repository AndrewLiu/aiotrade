package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Sec extends Table[Sec] with LongIdPK[Sec] {
  val validFrom = longColumn("validfrom")
  val validTo = longColumn("validTo").default("-1")

  val company   = longColumn("company_id")   references Company
  val secInfo   = longColumn("secInfo_id")   references SecInfo
  val secStatus = longColumn("secStatus_id") references SecStatus
  val secIssue  = longColumn("secIssue_id")  references SecIssue
}

class Sec extends Record[Sec](Sec) {
  val id = field(Sec.id)
  val validFrom = field(Sec.validFrom)
  val validTo = field(Sec.validTo)

  val company = manyToOne(Sec.company) // the current one. ont to one ?
  val companyHists = oneToMany(Company.sec)

  val secInfo = manyToOne(Sec.secInfo) // the current one. one to one ?
  val secInfoHists = oneToMany(SecInfo.sec)
  
  val secIssue = manyToOne(Sec.secIssue) // the current one. one to one ?
  val secStatus = oneToMany(SecStatus.sec)
  val secDividends = oneToMany(SecDividend.sec)
  
  val dailyQuotes  = oneToMany(Quote1d.sec)
  val minuteQuotes = oneToMany(Quote1m.sec)

  val dailyMonyFlow  = oneToMany(MoneyFlow1d.sec)
  val minuteMonyFlow = oneToMany(MoneyFlow1m.sec)

  val intraDays = oneToMany(IntraDay.sec)
}