package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object InnerDay extends Table[InnerDay] with LongIdPK[InnerDay] {
  val sec = longColumn("sec_id").references(Sec)
  val time = longColumn("time")
}

class InnerDay extends Record[InnerDay](InnerDay) {
  val id = field(InnerDay.id)
  val sec = manyToOne(InnerDay.sec)
  val time = field(InnerDay.time)

  val tickers = oneToMany(Ticker.innerDay)
  val bidAsks = oneToMany(BidAsk.innerDay)
  val dealRecords = oneToMany(DealRecord.innerDay)

  val moneyFlows = oneToMany(MoneyFlowTicker.innerDay)
}
