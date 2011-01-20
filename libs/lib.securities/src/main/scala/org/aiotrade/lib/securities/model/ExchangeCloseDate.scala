package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object ExchangeCloseDates extends Table[ExchangeCloseDate] {
  val exchange = "exchanges_id".BIGINT REFERENCES(Exchanges) //manyToOne(ExchangeCloseDate.exchange)

  val fromTime = "fromTime" BIGINT //field(ExchangeCloseDate.fromTime)
  val toTime = "toTime" BIGINT //field(ExchangeCloseDate.toTime)
}

class ExchangeCloseDate {
  var exchange: Exchange = _

  var fromTime: Long = _
  var toTime: Long = _
}
