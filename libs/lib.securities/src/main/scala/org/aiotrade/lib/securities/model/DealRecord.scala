package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object DealRecords extends Table[DealRecord] {
  val quote = "quotes_id" REFERENCES(Quotes1d) //manyToOne(DealRecord.quote)

  val time = "time" BIGINT //field(DealRecord.time)

  val price  = "price"  FLOAT(12, 2) //field(DealRecord.price)
  val volume = "volume" FLOAT(12, 2) //field(DealRecord.volume)
  val amount = "amount" FLOAT(12, 2) //field(DealRecord.amount)
}

class DealRecord {
  var quote: Quote = _
  
  var time: Long = -1

  var price: Float  = _
  var volume: Float = _
  var amount: Float = _
}
