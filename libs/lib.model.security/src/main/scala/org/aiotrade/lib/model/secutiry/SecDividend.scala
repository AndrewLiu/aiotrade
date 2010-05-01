package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object SecDividend extends Table[SecDividend] with LongIdPK[SecDividend] {
  val sec = longColumn("sec_id").references(Sec)
  val prevClose = numericColumn("prevClose", 12, 2)
  val adjWeight = numericColumn("adjWeight", 12, 2)
  val cashBonus = numericColumn("cashBonus", 12, 2)
  val shareBonus = numericColumn("shareBonus", 12, 2)
  val right = numericColumn("rights", 12, 2)
  val rightPrice = numericColumn("rightsPrice", 12, 2)
  val registerDate = longColumn("registerDate")
  val dividendDate = longColumn("dividendDate")
}

class SecDividend extends Record[SecDividend](SecDividend) {
  val id = field(SecDividend.id)
  val prevClose = field(SecDividend.prevClose)
  val adjWeight = field(SecDividend.adjWeight)
  val cashBonus = field(SecDividend.cashBonus)
  val shareBobus = field(SecDividend.shareBonus)
  val right = field(SecDividend.right)
  val rightPrice = field(SecDividend.rightPrice)
  val registerDate = field(SecDividend.registerDate)
  val dividendDate = field(SecDividend.dividendDate)
}

