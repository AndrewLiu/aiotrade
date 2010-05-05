package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object ExchangeCloseDate extends Table[ExchangeCloseDate] with LongIdPK[ExchangeCloseDate] {
  val fromTime = longColumn("fromTime")
  val toTime = longColumn("toTime")
  val exchange = longColumn("market_id").references(Exchange)

}

class ExchangeCloseDate extends Record[ExchangeCloseDate](ExchangeCloseDate) {
  val id = field(ExchangeCloseDate.id)
  val fromTime = field(ExchangeCloseDate.fromTime)
  val toTime = field(ExchangeCloseDate.toTime)
  val exchange = manyToOne(ExchangeCloseDate.exchange)
}
