package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object DealRecord extends Table[DealRecord] {
  val quote = "quote_id" REFERENCES(Quote1d) //manyToOne(DealRecord.quote)

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
