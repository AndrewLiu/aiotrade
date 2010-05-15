package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object SecInfos extends Table[SecInfo] {
  /**
   * Belongs to one Sec
   */
  val sec = "secs_id" REFERENCES(Secs) ON_DELETE CASCADE

  val validFrom = "validFrom" BIGINT
  val validTo = "validTo" BIGINT
  val uniSymbol = "uniSymbol" VARCHAR(10) DEFAULT("''")
  val name = "name" VARCHAR(40) DEFAULT("''")
  val totalShare = "totalShare" BIGINT
  val freeFloat = "freeFloat" BIGINT
  val tradingUnit = "tradingUnit" INTEGER 
  val upperLimit = "upperLimit" FLOAT(12, 2)
  val lowerLimit = "lowerLimit" FLOAT(12, 2)
}

class SecInfo {
  /**
   * Belongs to one Sec
   */
  var sec: Sec = _
  
  var validFrom: Long = _
  var validTo: Long = _
  var uniSymbol: String = ""
  var name: String = ""
  var totalShare: Long = _
  var freeFloat: Long = _
  var tradingUnit: Int = 100
  var upperLimit: Float = -1
  var lowerLimit: Float = -1
}
