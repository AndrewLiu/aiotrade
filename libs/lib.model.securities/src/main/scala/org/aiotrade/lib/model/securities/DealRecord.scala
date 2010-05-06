package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object DealRecord extends Table[DealRecord] with LongIdPK[DealRecord] {
  val quote = longColumn("quote_id").references(Quote1d)
  val time = longColumn("time")

  val price  = numericColumn("price",  12, 2)
  val volume = numericColumn("volume", 12, 2)
  val amount = numericColumn("amount", 12, 2)
}

class DealRecord extends Record[DealRecord](DealRecord) {
  val id = field(DealRecord.id)
  val quote = manyToOne(DealRecord.quote)
  val time = field(DealRecord.time)

  val price  = field(DealRecord.price)
  val volume = field(DealRecord.volume)
  val amount = field(DealRecord.amount)
}
