package org.aiotrade.lib.model.securities

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

  /**
   * Select latest ask_bid in each group of "isBid" and "idx"
   */
  def latest = {
    "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)"
  }

  def latest(innerDayId: Long) = {
    "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND innerDay_id = " + innerDayId + ") AND innerDay_id = " + innerDayId
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