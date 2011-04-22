package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object ExchangeCloseDates extends Table[ExchangeCloseDate] {
  val exchange = "exchanges_id" BIGINT() REFERENCES(Exchanges)

  val fromTime = "fromTime" BIGINT() 
  val toTime = "toTime" BIGINT()
}

class ExchangeCloseDate {
  var exchange: Exchange = _

  var fromTime: Long = _
  var toTime: Long = _
}
