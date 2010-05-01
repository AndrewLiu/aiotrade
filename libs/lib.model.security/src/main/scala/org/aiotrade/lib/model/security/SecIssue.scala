package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object SecIssue extends Table[SecIssue] with LongIdPK[SecIssue] {
  val sec = longColumn("sec_id").references(Sec)
  val par = numericColumn("par", 12, 2)
  val price = numericColumn("price", 12, 2)
  val totalShares = longColumn("totalShares")
  val issueDate = longColumn("issueDate")
  val listDate = longColumn("listDate")
}

class SecIssue extends Record[SecIssue](SecIssue) {
  val id = field(SecIssue.id)
  val par = field(SecIssue.par)
  val price = field(SecIssue.price)
  val totalShares = field(SecIssue.totalShares)
  val issueDate = field(SecIssue.issueDate)
  val listDate = field(SecIssue.listDate)
}
