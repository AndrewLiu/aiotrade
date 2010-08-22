package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object AnalysisReports extends Table[AnalysisReport]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
}

class AnalysisReport {
  var generalInfo : GeneralInfo = _
  
  var author : String = ""
  var publisher : String = ""
}