package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Market extends Table[Market] with LongIdPK[Market] {
  val name = stringColumn("name")
  val fullName = stringColumn("fullName", 30)
  val timeZone = stringColumn("timezone", 10)
  
  val openTime1  = longColumn("openTime1")
  val closeTime1 = longColumn("closeTime1")
  val openTime2  = longColumn("openTime2")
  val closeTime2 = longColumn("closeTime2")
}

class Market extends Record[Market](Market) {
  val id = field(Market.id)
  val name = field(Market.name)
  val fullName = field(Market.fullName)
  val openTime1 = field(Market.openTime1)
  val closeTime1 = field(Market.closeTime1)
  val openTime2 = field(Market.openTime2)
  val closeTime2 = field(Market.closeTime2)
  val closeDates = oneToMany(MarketCloseDate.market)
}
