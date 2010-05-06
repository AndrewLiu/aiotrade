package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Quote1d extends QuoteTable

object Quote1m extends QuoteTable

trait QuoteTable extends Table[Quote] with LongIdPK[Quote] {
  val sec = longColumn("sec_id").references(Sec)
  val time = longColumn("time")
  
  val open   = numericColumn("open",   12, 2)
  val high   = numericColumn("high",   12, 2)
  val low    = numericColumn("low",    12, 2)
  val close  = numericColumn("close",  12, 2)
  val volume = numericColumn("volume", 12, 2)
  val amount = numericColumn("amount", 12, 2)

  val adjWeight = numericColumn("adjWeight", 12, 2)

  val flag = intColumn("flag").default("0")
}

class Quote(table: QuoteTable) extends Record[Quote](table) {
  val id = field(table.id)
  val sec = manyToOne(table.sec)
  val time = field(table.time)
  
  val open   = field(table.open)
  val high   = field(table.high)
  val low    = field(table.low)
  val close  = field(table.close)
  val volume = field(table.volume)
  val amount = field(table.amount)
  
  val adjWeight = field(table.adjWeight)

  /**
   * 0 means unclosed
   * > 0 means closed
   * 0000,000X closed
   * 0000,00X0 verified
   */
  val flad = field(table.flag)

  // Foreign keys
  val tickers = oneToMany(Ticker.quote)
  val dealRecords = oneToMany(DealRecord.quote)
}
