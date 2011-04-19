package org.aiotrade.lib.securities.model

import scala.collection.immutable
import ru.circumflex.orm.Table

/**
 * fullcode is defined as two parts first part is caterogy (6 chars), second part is subcategory
 */
object Sector {
  object Category {
    // security kind
    val kind = "000000" 
    
    // industries
    val industryA = "008001"
    val IndustryB = "008002"
    val IndustryC = "008003"
    val IndustryD = "008004"
    val IndustryE = "008005"
  }
  
  object Code {
    // --- code of "kind"
    val index = "000"
    val stock = "001"
    val fund = "002"
    val option = "003"
    val bond = "004"
    val warrant = "005"
    val forex = "006"
  }
}

class Sector {
  var category: String = ""
  var code: String = ""
  var name: String = ""
  
  var secs: List[Sec] = Nil
}

object Sectors extends Table[Sector] {
  val category = "category" VARCHAR(6) DEFAULT("''")
  val code = "code" VARCHAR(20) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)
  
  val categoryIdx = getClass.getSimpleName + "_category_idx" INDEX(category.name)
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)
}


