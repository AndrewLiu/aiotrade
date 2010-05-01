package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object BidAsk extends Table[BidAsk] with LongIdPK[BidAsk] {
  val innerDay = longColumn("innerDay_id").references(InnerDay)
  val time = longColumn("time")

  val idx = intColumn("idx")
  val isBid = booleanColumn("isBid")
  val price = numericColumn("price",  12, 2)
  val size = numericColumn("size", 12, 2)
  val dealer = stringColumn("dealer", 30)

  def lastOne(innerDayId: Long) = {
    "select * from bid_ask where time = (select max(time) from bid_ask where innerDay_id = " + innerDayId + ")"
  }
}

class BidAsk extends Record[BidAsk](BidAsk) {
  val id = field(BidAsk.id)
  val innerDay = manyToOne(BidAsk.innerDay)
  val time = field(BidAsk.time)

  val idx  = field(BidAsk.idx)
  val isBid  = field(BidAsk.isBid)
  val price = field(BidAsk.price)
  val size = field(BidAsk.size)
  val dealer = field(BidAsk.dealer)
}