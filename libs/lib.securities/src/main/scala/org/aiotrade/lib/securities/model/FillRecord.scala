package org.aiotrade.lib.securities.model

import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.util.actors.Event

import ru.circumflex.orm.Table

case class FillRecordEvent(src: DataServer[_ <: TVal], fillRecords: Array[FillRecord]) extends Event

object FillRecords extends Table[FillRecord] {
  val quote = "quotes_id" REFERENCES(Quotes1d)

  val time = "time" BIGINT

  val price  = "price"  FLOAT(12, 2)
  val volume = "volume" FLOAT(12, 2)
  val amount = "amount" FLOAT(12, 2)
}

class FillRecord {
  var quote: Quote = _
  
  var time: Long = -1

  var price: Float  = _
  var volume: Float = _
  var amount: Float = _
}
