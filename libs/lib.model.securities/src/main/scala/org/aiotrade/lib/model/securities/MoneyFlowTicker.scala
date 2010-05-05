package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object MoneyFlowTicker extends Table[MoneyFlowTicker] with LongIdPK[MoneyFlowTicker] {
  val innerDay = longColumn("innerDay_id").references(InnerDay)
  val time = longColumn("time")

  val grade = intColumn("grade") // 1 - super, 2 - grand, 3 - small
  val volume = numericColumn("volume", 18, 2)
  val amount = numericColumn("amount", 18, 2)
}

class MoneyFlowTicker extends Record[MoneyFlowTicker](MoneyFlowTicker) {
  val id = field(MoneyFlowTicker.id)
  val innerDay = manyToOne(MoneyFlowTicker.innerDay)
  val time = field(MoneyFlowTicker.time)
  
  val grade = field(MoneyFlowTicker.grade)
  val volume = field(MoneyFlowTicker.volume)
  val amount = field(MoneyFlowTicker.amount)
}
