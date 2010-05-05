package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object DealRecord extends Table[DealRecord] with LongIdPK[DealRecord] {
  val innerDay = longColumn("innerDay_id").references(InnerDay)
  val time = longColumn("time")

  val price = numericColumn("price",  12, 2)
  val size = numericColumn("size", 12, 2)
  val dealer = stringColumn("dealer", 40)
}

class DealRecord extends Record[DealRecord](DealRecord) {
  val id = field(DealRecord.id)
  val innerDay = manyToOne(DealRecord.innerDay)
  val time = field(DealRecord.time)

  val price = field(DealRecord.price)
  val size = field(DealRecord.size)
  val dealer = field(DealRecord.dealer)
}
