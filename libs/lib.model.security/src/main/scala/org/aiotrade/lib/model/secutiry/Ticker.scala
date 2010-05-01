package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Ticker extends Table[Ticker] with LongIdPK[Ticker] {
  val innerDay = longColumn("innerDay_id").references(InnerDay)
  val time = longColumn("time")
  
  val prevClose  = numericColumn("prevClose",  12, 2)
  val lastPrice  = numericColumn("lastPrice",  12, 2)
  val lastVolume = numericColumn("lastVolume", 12, 2)
  val lastAmount = numericColumn("lastAmount", 12, 2)

  val dayOpen    = numericColumn("dayOpen",    12, 2)
  val dayHigh    = numericColumn("dayHigh",    12, 2)
  val dayLow     = numericColumn("dayLow",     12, 2)
  val dayVolume  = numericColumn("dayVolume",  12, 2)
  val dayAmount  = numericColumn("dayAmount",  12, 2)

  val dayChange  = numericColumn("dayChange",  12, 2)
}

class Ticker extends Record[Ticker](Ticker) {
  val id = field(Ticker.id)
  val innerDay = manyToOne(Ticker.innerDay)
  val time = field(Ticker.time)

  val prevClose  = field(Ticker.prevClose)
  val lastPrice  = field(Ticker.lastPrice)
  val lastVolume = field(Ticker.lastVolume)
  val lastAmount = field(Ticker.lastAmount)

  val dayOpen   = field(Ticker.dayOpen)
  val dayHigh   = field(Ticker.dayHigh)
  val dayLow    = field(Ticker.dayLow)
  val dayVolume = field(Ticker.dayVolume)
  val dayAmount = field(Ticker.dayAmount)

  val dayChange = field(Ticker.dayChange)
}
