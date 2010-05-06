package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object IntraDay extends Table[IntraDay] with LongIdPK[IntraDay] {
  val sec = longColumn("sec_id").references(Sec)
  val time = longColumn("time")
}

class IntraDay extends Record[IntraDay](IntraDay) {
  val id = field(IntraDay.id)
  val sec = manyToOne(IntraDay.sec)
  val time = field(IntraDay.time)

  val tickers = oneToMany(Ticker.intraDay)
  val dealRecords = oneToMany(DealRecord.intraDay)
}
