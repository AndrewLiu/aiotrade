package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object SecDividend extends Table[SecDividend] {
  val sec = "sec_id" REFERENCES(Sec) //manyToOne(SecDividend.sec)

  val prevClose = "prevClose" FLOAT(12, 2) //field(SecDividend.prevClose)
  val adjWeight =  "adjWeight" FLOAT(12, 2) //field(SecDividend.adjWeight)
  val cashBonus = "cashBonus" FLOAT(12, 2) //field(SecDividend.cashBonus)
  val shareBobus = "shareBonus" FLOAT(12, 2) //field(SecDividend.shareBonus)
  val shareRight =  "shareRight" FLOAT(12, 2) //field(SecDividend.right)
  val shareRightPrice = "shareRightPrice" FLOAT(12, 2) //field(SecDividend.rightPrice)
  val registerDate = "registerDate" BIGINT //field(SecDividend.registerDate)
  val dividendDate = "dividendDate" BIGINT //field(SecDividend.dividendDate)
}

class SecDividend {
  var sec: Sec = _

  var prevClose: Float = _
  var adjWeight: Float = _
  var cashBonus: Float = _
  var shareBobus: Float = _
  var shareRight: Float = _
  var shareRightPrice: Float = _
  var registerDate: Long = _
  var dividendDate: Long = _
}

