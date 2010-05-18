package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object MoneyFlows1d extends MoneyFlows
object MoneyFlows1m extends MoneyFlows

abstract class MoneyFlows extends Table[MoneyFlow] {
  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val totalVolume = "totalVolume" FLOAT(18, 2)
  val totalAmount = "totalAmount" FLOAT(18, 2)

  val superVolume = "superVolume" FLOAT(18, 2)
  val superAmount = "superAmount" FLOAT(18, 2)

  val largeVolume = "largeVolume" FLOAT(18, 2)
  val largeAmount = "largeAmount" FLOAT(18, 2)

  val smallVolume = "smallVolume" FLOAT(18, 2)
  val smallAmount = "smallAmount" FLOAT(18, 2)
}


/**
 * The definition of "super/large/small block" will depond on amount
 */
class MoneyFlow {
  var sec: Sec = _
  
  var time: Long = _

  var totalVolume: Float = _
  var totalAmount: Float = _

  var superVolume: Float = _
  var superAmount: Float = _

  var largeVolume: Float = _
  var largeAmount: Float = _

  var smallVolume: Float = _
  var smallAmount: Float = _
}
