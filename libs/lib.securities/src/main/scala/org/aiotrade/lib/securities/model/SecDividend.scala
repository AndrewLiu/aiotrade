package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecDividends extends Table[SecDividend] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val prevClose = "prevClose" DOUBLE()
  val adjWeight = "adjWeight" DOUBLE()
  val cashBonus = "cashBonus" DOUBLE()
  val shareBonus = "shareBonus" DOUBLE()
  val shareRight = "shareRight" DOUBLE()
  val sharePrice = "shareRightPrice" DOUBLE()
  val registerDate = "registerDate" BIGINT()
  val dividendDate = "dividendDate" BIGINT()
}

class SecDividend {
  var sec: Sec = _

  var prevClose: Double = _
  var adjWeight: Double = _
  var cashBonus: Double = _
  var shareBonus: Double = _ // bonus issue, entitle bonus share
  var shareRight: Double = _ // allotment of shares in sharePrice
  var sharePrice: Double = _ // price of allotment of share
  var registerDate: Long = _
  var dividendDate: Long = _
  
  final def dividedClose = accurateAdjust(prevClose)
  
  private def cashAfterwards = cashBonus - shareRight * sharePrice
  private def shareAfterwards = 1 + shareRight + shareBonus

  final def accurateAdjust(price: Double)   = (price - cashBonus + shareRight * sharePrice) / (1 + shareRight + shareBonus)
  final def accurateUnadjust(price: Double) = price * (1 + shareRight + shareBonus) + (cashBonus - shareRight * sharePrice)
  
  final def adjust(price: Double) = {
    val p = accurateAdjust(price)
    if (p != price) p else (if (adjWeight != 0) price / adjWeight else price)
  }
  
  final def unadjust(price: Double) = {
    val p = accurateUnadjust(price)
    if (p != price) p else (if (adjWeight != 0) price * adjWeight else price)
  }
  
  final def forwardAdjust(price: Double) = adjust(price)
  final def backwradAdjust(price: Double) = unadjust(price)
}

