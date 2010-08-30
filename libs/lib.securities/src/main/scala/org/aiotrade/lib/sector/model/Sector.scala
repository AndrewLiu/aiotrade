package org.aiotrade.lib.sector.model

import ru.circumflex.orm._
import scala.collection.mutable.HashMap

object Sectors extends Table[Sector] {
  val name = "name" VARCHAR(30)
  val code = "code" VARCHAR(30)
  var portfolio = "portfolios_id" REFERENCES(Portfolios)

  private var isLoad : Boolean = false
  private val codetoSector   = new HashMap[String, Sector]()

  def sectorOf(code : String) : Option[Sector] = {
    synchronized {
      if(!isLoad){
        load
        isLoad = true
      }
      codetoSector.get(code)
    }

  }
  private def load {
    val sectors = (SELECT (Sectors.*) FROM Sectors list)
    for (sector <- sectors){
      codetoSector.put(sector.code, sector)
    }
  }
  def portfolios = inverse(Portfolios.sector)
}

class Sector {
  var name : String = ""
  var code : String = ""
  var portfolio : Portfolio = _
  var portfolios : List[Portfolio] = Nil
}
