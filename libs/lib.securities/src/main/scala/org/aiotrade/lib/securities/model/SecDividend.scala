package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecDividends extends Table[SecDividend] {
  val sec = "secs_id" REFERENCES(Secs)

  val prevClose = "prevClose" DOUBLE()
  val adjWeight = "adjWeight" DOUBLE()
  val cashBonus = "cashBonus" DOUBLE()
  val shareBobus = "shareBonus" DOUBLE()
  val shareRight = "shareRight" DOUBLE()
  val shareRightPrice = "shareRightPrice" DOUBLE()
  val registerDate = "registerDate" BIGINT
  val dividendDate = "dividendDate" BIGINT
}

class SecDividend {
  var sec: Sec = _

  var prevClose: Double = _
  var adjWeight: Double = _
  var cashBonus: Double = _
  var shareBobus: Double = _
  var shareRight: Double = _
  var shareRightPrice: Double = _
  var registerDate: Long = _
  var dividendDate: Long = _
}

