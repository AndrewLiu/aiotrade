package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Exchange extends Table[Exchange] with LongIdPK[Exchange] {
  val name = stringColumn("name")
  val fullName = stringColumn("fullName", 30)
  val timeZone = stringColumn("timezone", 10)
  
  val openTime1  = longColumn("openTime1")
  val closeTime1 = longColumn("closeTime1")
  val openTime2  = longColumn("openTime2")
  val closeTime2 = longColumn("closeTime2")
}

class Exchange extends Record[Exchange](Exchange) {
  val id = field(Exchange.id)
  val name = field(Exchange.name)
  val fullName = field(Exchange.fullName)
  val openTime1 = field(Exchange.openTime1)
  val closeTime1 = field(Exchange.closeTime1)
  val openTime2 = field(Exchange.openTime2)
  val closeTime2 = field(Exchange.closeTime2)
  val closeDates = oneToMany(ExchangeCloseDate.exchange)
}
