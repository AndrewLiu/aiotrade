package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecStatuses extends Table[SecStatus] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val validFrom = "validFrom" BIGINT()
  val validTo = "validTo" BIGINT()
  val suspension = "suspension" INTEGER()
  val loanDirection = "loanDirection" INTEGER()
  val dividStatus = "dividStatus" INTEGER()
}

class SecStatus {
  var sec: Sec = _

  var validFrom: Long = _
  var validTo: Long = _
  var suspension: Int = _
  var loanDirection: Int = _
  var dividStatus: Int = _
}
