package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object MoneyFlow1d extends MoneyFlowTable
object MoneyFlow1m extends MoneyFlowTable

trait MoneyFlowTable extends Table[MoneyFlow] with LongIdPK[MoneyFlow] {
  val sec = longColumn("sec_id").references(Sec)

  val time = longColumn("time")

  val totalVolume = numericColumn("totalVolume", 18, 2)
  val totalAmount = numericColumn("totalAmount", 18, 2)

  val superVolume = numericColumn("superVolume", 18, 2)
  val superAmount = numericColumn("superAmount", 18, 2)

  val grandVolume = numericColumn("grandVolume", 18, 2)
  val grandAmount = numericColumn("grandAmount", 18, 2)

  val smallVolume = numericColumn("smallVolume", 18, 2)
  val smallAmount = numericColumn("smallAmount", 18, 2)
}

class MoneyFlow(table: MoneyFlowTable) extends Record[MoneyFlow](table) {
  val id = field(table.id)
  val sec = manyToOne(table.sec)
  val time = field(table.time)

  val totalVolume = field(table.totalVolume)
  val totalAmount = field(table.totalAmount)

  val superVolume = field(table.superVolume)
  val superAmount = field(table.superAmount)

  val grandVolume = field(table.grandVolume)
  val grangAmount = field(table.grandAmount)

  val smallVolume = field(table.smallVolume)
  val smallAmount = field(table.smallAmount)
}
