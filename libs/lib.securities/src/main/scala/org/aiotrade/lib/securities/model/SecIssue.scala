package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecIssue extends Table[SecIssue] {
  val sec = "sec_id" REFERENCES(Sec) //oneToOne(Sec.secIssue)

  val par = "par" FLOAT(12, 2) //field(SecIssue.par)
  val price = "price" FLOAT(12, 2) //field(SecIssue.price)
  val totalShare = "totalShare" BIGINT //field(SecIssue.totalShares)
  val issueDate = "issueDate" BIGINT //field(SecIssue.issueDate)
  val listDate = "listDate" BIGINT //field(SecIssue.listDate)
}

class SecIssue {
  var sec: Sec = _

  var par: Float = _
  var price: Float = _
  var totalShare: Long = _
  var issueDate: Long = _
  var listDate: Long = _
}
