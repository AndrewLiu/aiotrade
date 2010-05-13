package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object MoneyFlow1d extends MoneyFlowTable
object MoneyFlow1m extends MoneyFlowTable

abstract class MoneyFlowTable extends Table[MoneyFlow] {
  val sec = "sec_id" REFERENCES(Sec) //manyToOne(table.sec)

  val time = "time" BIGINT //field(table.time)

  val totalVolume = "totalVolume" FLOAT(18, 2) //field(table.totalVolume)
  val totalAmount = "totalAmount" FLOAT(18, 2) //field(table.totalAmount)

  val superVolume = "superVolume" FLOAT(18, 2) //field(table.superVolume)
  val superAmount = "superAmount" FLOAT(18, 2) //field(table.superAmount)

  val grandVolume = "grandVolume" FLOAT(18, 2) //field(table.grandVolume)
  val grangAmount = "grandAmount" FLOAT(18, 2) //field(table.grandAmount)

  val smallVolume = "smallVolume" FLOAT(18, 2) //field(table.smallVolume)
  val smallAmount = "smallAmount" FLOAT(18, 2) //field(table.smallAmount)
}

class MoneyFlow {
  var sec: Sec = _
  
  var time: Long = _

  var totalVolume: Float = _
  var totalAmount: Float = _

  var superVolume: Float = _
  var superAmount: Float = _

  var grandVolume: Float = _
  var grangAmount: Float = _

  var smallVolume: Float = _
  var smallAmount: Float = _
}
