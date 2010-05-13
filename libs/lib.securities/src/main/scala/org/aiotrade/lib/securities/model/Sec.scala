package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Sec extends Table[Sec] {
  val exchange = "exchange_id" REFERENCES(Exchange)

  val validFrom = "validFrom" BIGINT //field(Sec.validFrom)
  val validTo = "validTo" BIGINT //field(Sec.validTo)

  val company = "company_id" REFERENCES(Company) //manyToOne(Sec.company) // the current one. ont to one ?
  def companyHists = inverse(Company.sec)//oneToMany(Company.sec)

  val secInfo = "secInfo_id" REFERENCES(SecInfo) // manyToOne(Sec.secInfo) // the current one. one to one ?
  def secInfoHists = inverse(SecInfo.sec) //oneToMany(SecInfo.sec)
  val secStatus = "secStatus_id" REFERENCES(SecStatus)
  def secStatusHists = inverse(SecStatus.sec) //oneToMany(SecStatus.sec)

  val secIssue = "secIssue_id" REFERENCES(SecIssue) //manyToOne(Sec.secIssue) // the current one. one to one ?
  def secDividends = inverse(SecDividend.sec) //oneToMany(SecDividend.sec)

  def dailyQuotes = inverse(Quote1d.sec)
  def dailyMoneyFlow = inverse(MoneyFlow1d.sec)

  def minuteQuotes = inverse(Quote1m.sec)
  def minuteMoneyFlow = inverse(MoneyFlow1m.sec)
}

class Sec {
  var exchange: Exchange = _

  var validFrom: Long = 0
  var validTo: Long = 0

  var company: Company = _
  var companyHists: List[Company] = Nil

  var secInfo: SecInfo = _
  var secInfoHists: List[SecInfo] = Nil
  var secStatus: SecStatus = _
  var secStatusHists: List[SecStatus] = Nil

  var secIssue: SecIssue = _
  var secDividends: List[SecDividend] = Nil

  var dailyQuotes: List[Quote] = Nil
  var dailyMoneyFlow: List[MoneyFlow] = Nil

  var minuteQuotes: List[Quote] = Nil
  var minuteMoneyFlow: List[MoneyFlow] = Nil
}