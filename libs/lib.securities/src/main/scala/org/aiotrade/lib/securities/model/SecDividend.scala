package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecDividends extends Table[SecDividend] {
  val sec = "secs_id" REFERENCES(Secs)

  val prevClose = "prevClose" FLOAT(12, 2) 
  val adjWeight =  "adjWeight" FLOAT(12, 2)
  val cashBonus = "cashBonus" FLOAT(12, 2)
  val shareBobus = "shareBonus" FLOAT(12, 2)
  val shareRight =  "shareRight" FLOAT(12, 2) 
  val shareRightPrice = "shareRightPrice" FLOAT(12, 2)
  val registerDate = "registerDate" BIGINT
  val dividendDate = "dividendDate" BIGINT
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

