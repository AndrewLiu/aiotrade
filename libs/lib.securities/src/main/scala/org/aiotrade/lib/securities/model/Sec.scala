package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Sec extends Table[Sec] {
  val exchange = "exchange_id" REFERENCES(Exchange)

  val validFrom = "validFrom" BIGINT 
  val validTo = "validTo" BIGINT

  val company = "company_id" REFERENCES(Company)
  def companyHists = inverse(Company.sec)

  val secInfo = "secInfo_id" REFERENCES(SecInfo) 
  def secInfoHists = inverse(SecInfo.sec) 
  val secStatus = "secStatus_id" REFERENCES(SecStatus)
  def secStatusHists = inverse(SecStatus.sec)

  val secIssue = "secIssue_id" REFERENCES(SecIssue)
  def secDividends = inverse(SecDividend.sec)

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