package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecStatus extends Table[SecStatus] {
  val sec = "sec_id" REFERENCES(Sec) // manyToOne(SecStatus.sec)

  val validFrom = "validFrom" BIGINT // field(SecStatus.validFrom)
  val validTo = "validTo" BIGINT //field(SecStatus.validTo)
  val suspension = "suspension" INTEGER //field(SecStatus.suspension)
  val loanDirection = "loanDirection" INTEGER //field(SecStatus.loanDirection)
  val dividStatus = "dividStatus" INTEGER //field(SecStatus.dividStatus)
}

class SecStatus {
  var sec: Sec = _

  var validFrom: Long = _
  var validTo: Long = _
  var suspension: Int = _
  var loanDirection: Int = _
  var dividStatus: Int = _
}
