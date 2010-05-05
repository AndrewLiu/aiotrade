package org.aiotrade.lib.model.securities

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object SecStatus extends Table[SecStatus] with LongIdPK[SecStatus] {
  val sec = longColumn("sec_id").references(Sec)
  val validFrom = longColumn("validfrom")
  val validTo = longColumn("validTo")
  val valid = booleanColumn("valid")
  val suspension = intColumn("suspension")
  val loanDirection = intColumn("loanDirection")
  val dividStatus = intColumn("dividStatus")
}

class SecStatus extends Record[SecStatus](SecStatus) {
  val id = field(SecStatus.id)
  val sec = manyToOne(SecStatus.sec)
  val validFrom = field(SecStatus.validFrom)
  val validTo = field(SecStatus.validTo)
  val valid = field(SecStatus.valid)
  val suspension = field(SecStatus.suspension)
  val loanDirection = field(SecStatus.loanDirection)
  val dividStatus = field(SecStatus.dividStatus)
}
