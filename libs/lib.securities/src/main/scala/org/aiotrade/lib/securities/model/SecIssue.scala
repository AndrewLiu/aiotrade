package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object SecIssues extends Table[SecIssue] {
  val sec = "secs_id" REFERENCES(Secs)

  val par = "par" FLOAT()
  val price = "price" FLOAT()
  val totalShare = "totalShare" BIGINT
  val issueDate = "issueDate" BIGINT
  val listDate = "listDate" BIGINT
}

class SecIssue {
  var sec: Sec = _

  var par: Float = _
  var price: Float = _
  var totalShare: Long = _
  var issueDate: Long = _
  var listDate: Long = _
}
