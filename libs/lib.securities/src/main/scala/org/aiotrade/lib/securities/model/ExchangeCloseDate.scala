package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object ExchangeCloseDate extends Table[ExchangeCloseDate] {
  val exchange = "exchange_id" REFERENCES(Exchange) //manyToOne(ExchangeCloseDate.exchange)

  val fromTime = "fromTime" BIGINT //field(ExchangeCloseDate.fromTime)
  val toTime = "toTime" BIGINT //field(ExchangeCloseDate.toTime)
}

class ExchangeCloseDate {
  var exchange: Exchange = _

  var fromTime: Long = _
  var toTime: Long = _
}
