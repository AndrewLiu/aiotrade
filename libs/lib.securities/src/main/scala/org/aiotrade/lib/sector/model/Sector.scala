package org.aiotrade.lib.sector.model

import ru.circumflex.orm._
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.info.model.InfoContent
import org.aiotrade.lib.math.timeseries.TVal
import scala.collection.JavaConversions._

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

class Sector extends TVal with Flag with InfoContent{
  var name : String = ""
  var code : String = ""
  var portfolio : Portfolio = _
  var portfolios : List[Portfolio] = Nil

  def publishTime: Long = this.time
  def weight: Float = 0F
  def link: String = ""

  def exportToMap: Map[String, String] = Map("" -> "")

  def exportToJavaMap: java.util.Map[String, String] = exportToMap

  def exportToList: Buffer[Map[String, String]] = {

    val list = Buffer[Map[String, String]]()

    if(portfolio != null && portfolio.breakouts != null){
      for(portfolio <- portfolio.breakouts){
        val map = Map[String, String]("SECURITY_CODE" -> portfolio.sec.uniSymbol)
        map += ("SECURITY_NAME" -> portfolio.sec.secInfo.name)
        map += ("ENTER_TIME" -> this.time.toString)
        list += map
      }
    }
    list
  }

  def exportToJavaList: java.util.List[java.util.Map[String, String]] = {
    
    val list = new java.util.ArrayList[java.util.Map[String, String]]
    
    if(portfolio != null && portfolio.breakouts != null){
      for(portfolio <- portfolio.breakouts){
        val map = Map[String, String]("SECURITY_CODE" -> portfolio.sec.uniSymbol)
        map += ("SECURITY_NAME" -> portfolio.sec.secInfo.name)
        map += ("ENTER_TIME" -> this.time.toString)
        list.add(map)
      }
    }
    list
  }
}
