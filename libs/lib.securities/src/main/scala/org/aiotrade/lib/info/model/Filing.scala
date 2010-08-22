package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object Filings extends Table[Filing]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)
  
  val publisher = "publisher" VARCHAR(30) DEFAULT("''")
  val format = "format" TINYINT
  val size = "size" BIGINT
}

class Filing {
  var generalInfo : GeneralInfo = _

  var publisher : String = ""
  var format : Int = _ 
  var size : Long = _

  object Format {
    val pdf = 1
    val txt = 2
    val word = 3
    val others = 99
  }
}


