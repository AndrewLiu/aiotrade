package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object Exchange extends Table[Exchange] {
  val name = "name" VARCHAR(10) DEFAULT("''") //field(Exchange.name)
  val fullName = "fullName" VARCHAR(30) DEFAULT("''") //field(Exchange.fullName)
  val openTime1 = "openTime1" BIGINT //field(Exchange.openTime1)
  val closeTime1 = "closeTime1" BIGINT //field(Exchange.closeTime1)
  val openTime2 = "openTime2" BIGINT //field(Exchange.openTime2)
  val closeTime2 = "closeTime2" BIGINT //field(Exchange.closeTime2)

  def closeDates = inverse(ExchangeCloseDate.exchange) //oneToMany(ExchangeCloseDate.exchange)
  def secs = inverse(Sec.exchange)
}

class Exchange {
  var name: String = ""
  var fullName: String = ""
  var openTime1: Long = _
  var closeTime1: Long = _
  var openTime2: Long = _
  var closeTime2: Long = _

  var closeDates: List[ExchangeCloseDate] = Nil
  var secs: List[Sec] = Nil
}
