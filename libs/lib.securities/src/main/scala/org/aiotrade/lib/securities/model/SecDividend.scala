package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecDividends extends Table[SecDividend] {
  val sec = "secs_id" REFERENCES(Secs)

  val prevClose = "prevClose" FLOAT() 
  val adjWeight =  "adjWeight" FLOAT()
  val cashBonus = "cashBonus" FLOAT()
  val shareBobus = "shareBonus" FLOAT()
  val shareRight =  "shareRight" FLOAT() 
  val shareRightPrice = "shareRightPrice" FLOAT()
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

