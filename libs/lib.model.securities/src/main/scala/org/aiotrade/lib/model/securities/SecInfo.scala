package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object SecInfo extends Table[SecInfo] with LongIdPK[SecInfo] {
  val sec = longColumn("sec_id").references(Sec)
  val validFrom = longColumn("validfrom")
  val validTo = longColumn("validTo")
  val valid = booleanColumn("valid")

  val symbol = stringColumn("symbol", 10).notNull//.unique.validateNotEmpty.validatePattern("^[a-zA-Z]{1,8}$")
  val name = stringColumn("name", 40)
  val totalShares = longColumn("totalShares")
  val freeFloat = longColumn("freeFloat")
  val tradingUnit = intColumn("tradingUnit")
  val upperLimit = numericColumn("upperLimit", 12, 2)
  val lowerLimit = numericColumn("lowerLimit", 12, 2)
}

class SecInfo extends Record[SecInfo](SecInfo) {
  val id = field(SecInfo.id)
  val sec = manyToOne(SecInfo.sec)
  val validFrom = field(SecInfo.validFrom)
  val validTo = field(SecInfo.validTo)
  val valid = field(SecInfo.valid)
  val symbol = field(SecInfo.symbol)
  val name = field(SecInfo.name)
  val totalShare = field(SecInfo.totalShares)
  val freeFloat = field(SecInfo.freeFloat)
  val tradingUnit = field(SecInfo.tradingUnit)
  val upperLimit = field(SecInfo.upperLimit)
  val lowerLimit = field(SecInfo.lowerLimit)
}
