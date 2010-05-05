package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object MarketCloseDate extends Table[MarketCloseDate] with LongIdPK[MarketCloseDate] {
  val fromTime = longColumn("fromTime")
  val toTime = longColumn("toTime")
  val market = longColumn("market_id").references(Market)

}

class MarketCloseDate extends Record[MarketCloseDate](MarketCloseDate) {
  val id = field(MarketCloseDate.id)
  val fromTime = field(MarketCloseDate.fromTime)
  val toTime = field(MarketCloseDate.toTime)
  val market = manyToOne(MarketCloseDate.market)
}
