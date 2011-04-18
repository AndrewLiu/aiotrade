package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object Sectors extends Table[Sector] {
  val code = "code" VARCHAR(10) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)
  
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)
}

class Sector {
  var code: String = ""
  var name: String = ""
  
  var secs: List[Sec] = Nil
}

