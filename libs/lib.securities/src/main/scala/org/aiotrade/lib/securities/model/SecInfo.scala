package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object SecInfo extends Table[SecInfo] {
  /**
   * Belongs to one Sec
   */
  val sec = "sec_id" REFERENCES(Sec) ON_DELETE CASCADE //manyToOne(SecInfo.sec)

  val validFrom = "validFrom" BIGINT //field(SecInfo.validFrom)
  val validTo = "validTo" BIGINT //field(SecInfo.validTo)
  val symbol = "symbol" VARCHAR(10) DEFAULT("''") //field(SecInfo.symbol)
  val name = "name" VARCHAR(40) DEFAULT("''") //field(SecInfo.name)
  val totalShare = "totalShare" BIGINT //field(SecInfo.totalShares)
  val freeFloat = "freeFloat" BIGINT //field(SecInfo.freeFloat)
  val tradingUnit = "tradingUnit" INTEGER //field(SecInfo.tradingUnit)
  val upperLimit = "upperLimit" FLOAT(12, 2)  //field(SecInfo.upperLimit)
  val lowerLimit = "lowerLimit" FLOAT(12, 2)  //field(SecInfo.lowerLimit)
}

class SecInfo {
  /**
   * Belongs to one Sec
   */
  var sec: Sec = _
  
  var validFrom: Long = _
  var validTo: Long = _
  var symbol: String = ""
  var name: String = ""
  var totalShare: Long = _
  var freeFloat: Long = _
  var tradingUnit: Int = _
  var upperLimit: Float = _
  var lowerLimit: Float = _
}
