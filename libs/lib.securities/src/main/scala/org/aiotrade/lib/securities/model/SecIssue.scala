package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecIssues extends Table[SecIssue] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val par = "par" DOUBLE()
  val price = "price" DOUBLE()
  val totalShare = "totalShare" BIGINT()
  val issueDate = "issueDate" BIGINT()
  val listDate = "listDate" BIGINT()
}

class SecIssue extends BelongsToSec {
  var par: Double = _
  var price: Double = _
  var totalShare: Long = _
  var issueDate: Long = _
  var listDate: Long = _
}
