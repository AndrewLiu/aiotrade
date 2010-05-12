package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object Quote1d extends QuoteTable
object Quote1m extends QuoteTable

abstract class QuoteTable extends Table[Quote] {
  val sec = "sec_id" REFERENCES(Sec)

  val time = "time" BIGINT

  val open   = "open"   FLOAT(12, 2)
  val high   = "high"   FLOAT(12, 2)
  val low    = "low"    FLOAT(12, 2)
  val close  = "close"  FLOAT(12, 2)
  val volume = "volume" FLOAT(12, 2)
  val amount = "amount" FLOAT(12, 2)

  val adjWeight = "adjWeight" FLOAT(12, 2)

  val flag = "flag" INTEGER

  // Foreign keys
  def tickers = inverse(Ticker.quote)
  def dealRecords = inverse(Ticker.quote)
}

class Quote {
  var sec: Sec = _

  var time: Long = _

  var open:   Float = _
  var high:   Float = _
  var low:    Float = _
  var close:  Float = _
  var volume: Float = _
  var amount: Float = _

  var adjWeight: Float   = _

  /**
   * 0 means unclosed
   * > 0 means closed
   * 0000,000X closed
   * 0000,00X0 verified
   */
  var flag: Int = 0

  // Foreign keys
  var tickers: List[Ticker] = Nil
  var dealRecords: List[Ticker] = Nil
}
