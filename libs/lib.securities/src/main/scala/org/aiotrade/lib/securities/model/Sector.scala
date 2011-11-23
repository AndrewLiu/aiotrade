/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of AIOTrade Computing Co. nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.securities.model

import scala.collection
import scala.collection.mutable
import scala.collection.immutable
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.ValidTime
import ru.circumflex.orm._

/**
 * Fullcode is defined as two parts:
 *   1st part is caterogy (6 chars), 
 *   2nd part is code/subcategory (max 20 chars)
 * 
 * @author Caoyuan Deng
 */
@serializable
class Sector extends CRCLongId {
  var category: String = ""
  var code: String = ""
  var name: String = ""
  
//  var secs: List[Sec] = Nil
//  var Children: Seq[Sector] = Nil
  var childrenString: String = ""
  
  lazy val key = Sector.toKey(category, code)
  
  override def hashCode = id.hashCode
  
  override def equals(that: Any) = that match {
    case x: Sector => this.crckey == x.crckey
    case _ => false
  }

  def copyFrom(another: Sector){
    this.code = another.code
    this.name = another.name
    this.crckey = another.crckey
    this.childrenString = another.childrenString
  }
}

object Sector {

  // max 6 chars
  object Category {
    
    // security kind
    val Kind = "KIND" 
    
    // exchange
    val Exchange = "EXCHAN"
    
    // industries
    val IndustryA = "008001"
    val IndustryB = "008002"
    val IndustryC = "008003"
    val IndustryD = "008004"
    val IndustryE = "008005"

    // tdx industries
    val TDXIndustries = Array("008011","008012","008013","008014","008015","008018")
    
    // boards
    val Board = "BOARD"
    
    // joint
    val Joint = "JOINT" 

    // custom
    val Custom = "CUSTOM"
  }
  
  // --- subcategories/code
  
  object Kind {
    val Index = "INDEX"                 // 指数
    val Stock = "STOCK"                 // 股票
    val Fund = "FUND"                  // 基金
    val Bond = "BOND"                  // 债券
    val Warrant = "WARRANT"               // 权证
    val Future = "FUTURE"                // 期货
    val Forex = "FOREX"                 // 外汇
    val Option = "OPTION"                // 期权
    val Treasury = "TREASURY"              // 国债
    val AdditionalShareOffer = "ADDSHAOFFER"  // 增发
    val ConvertibleBond = "CONVBOND"       // 可转换债券    
    val TreasuryRepurchase = "TREASREP"    // 国债回购
  }
  
  // --- code of 'board'
  object Board {
    val Main = "MAIN"
    
    val AShare = "ASHARE"
    val BShare = "BSHARE"
    val HShare = "HSHARE"

    val SME = "SME" // Small and Medium-sized Enterprised board
    val GEM = "GEM" // Growth Enterprises Market board
  }
  
  lazy val sectorToSecValidTimes: collection.Map[String, collection.Seq[ValidTime[Sec]]] = {
    Sectors.sectorToSecValidTimes
  }

  def toKey(category: String, code: String): String = category + "." + code
  def toCategoryCode(key: String): (String, String) = {
    val separator = key.indexOf('.')
    if (separator > 0) {
      val category = key.substring(0, separator)
      val code = key.substring(separator + 1, key.length)
      (category, code)
    } else {
      (key, null)
    }
  }
  
  def allSectors = sectorToSecValidTimes.keys
  
  def sectorsOf(category: String) = Sectors.sectorsOf(category)
  def sectorsOf = Sectors.sectorsOf
  
  def secsOf(sector: Sector): Seq[Sec] = Sectors.secsOf(sector)
  def secsOf(key: String): Seq[Sec] = withKey(key) match {
    case Some(sector) => secsOf(sector)
    case None => Nil
  }
  
  def withKey(key: String): Option[Sector] = Sectors.withKey(key)
  def withCategoryCode(category: String, code: String): Option[Sector] = Sectors.withCategoryCode(category, code)
  
  def cnSymbolToSectorKey(uniSymbol: String): Seq[String] = {
    var sectorKeys = List[String]()
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol, "SS") => 
        sectorKeys ::= toKey(Category.Exchange, "SS")
        
        if (symbol.startsWith("000")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Index)
        } else if (symbol.startsWith("009") || symbol.startsWith("010") || symbol.startsWith("020")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury)
        } else if (symbol.startsWith("600") || symbol.startsWith("601")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("900")) { 
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("500") || symbol.startsWith("510")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Fund)
        } else if (symbol.startsWith("580")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
        } else if (symbol.startsWith("100") || symbol.startsWith("110") || symbol.startsWith("112") || symbol.startsWith("113")) {
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("120") || symbol.startsWith("129")) { // enterprised bond
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("1")) { 
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        }

      case Array(symbol, "SZ") => 
        sectorKeys ::= toKey(Category.Exchange, "SZ")
        
        if (symbol.startsWith("00")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("03")) { // 认购或认沽权证
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("07")) {
          sectorKeys ::= toKey(Category.Kind, Kind.AdditionalShareOffer)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("08")) { // 配股权证
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("101")) { // 国债券挂牌分销
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury) 
        } else if (symbol.startsWith("109")) { // 地方政府债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond) 
        } else if (symbol.startsWith("10")) {  // 国债现货
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury)
        } else if (symbol.startsWith("111")) { // 企业债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("112")) { // 公司债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("115")) { // 分离交易型可转债
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("12")) {
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("13")) {
          sectorKeys ::= toKey(Category.Kind, Kind.TreasuryRepurchase)
        } else if (symbol.startsWith("15") || symbol.startsWith("16") || symbol.startsWith("18")) { 
          sectorKeys ::= toKey(Category.Kind, Kind.Fund)
        } else if (symbol.startsWith("20")) { 
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("28")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("30")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("37")) {
          sectorKeys ::= toKey(Category.Kind, Kind.AdditionalShareOffer)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("38")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("39")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Index)
        }
      case _ => 
    }
    sectorKeys  
  }
  
  def apply(category: String, code: String) = new Sector()
  
  // --- simple test
  def main(args: Array[String]) {
    try {
      val secsHolder = SELECT(Secs.*) FROM (Secs) list()
      val t0 = System.currentTimeMillis
      val sectorToSecValidTimes = Sectors.sectorToSecValidTimes
      sectorToSecValidTimes foreach println
      println("Finished in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
      System.exit(0)
    } catch {
      case ex => ex.printStackTrace; System.exit(1)
    }
  }
}


// --- table
object Sectors extends CRCLongPKTable[Sector] {
  private val log = Logger.getLogger(this.getClass.getName)
  
  val category = "category" VARCHAR(6) DEFAULT("''")
  val code = "code" VARCHAR(20) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")
  val childrenString = "children" VARCHAR(2048) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)

  val categoryIdx = getClass.getSimpleName + "_category_idx" INDEX(category.name)
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)
  
  
  // --- helpers:
  
  private[model] def allSectors: Seq[String] = {
    val res = try {
      SELECT (Sectors.*) FROM (Sectors) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res map (_.key)
  }
  
  private[model] def sectorsOf(category: String): Seq[Sector] = {
    try {
      SELECT (Sectors.*) FROM (Sectors) WHERE (Sectors.category EQ category) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  } 

  private[model] def sectorsOf(): Seq[Sector] = {
    SELECT (Sectors.*) FROM (Sectors) list()
  }

  private[model] def secsOf(sector: Sector): Seq[Sec] = {
    try {
      SELECT (Secs.*) FROM (SectorSecs JOIN Secs) WHERE (SectorSecs.sector.field EQ Sectors.idOf(sector)) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }
  
  private[model] def withKey(key: String): Option[Sector] = {
    val (category, code) = Sector.toCategoryCode(key)
    withCategoryCode(category, code)
  }
  
  private[model] def withCategoryCode(category: String, code: String): Option[Sector] = {
    try {
      SELECT (Sectors.*) FROM (Sectors) WHERE ((Sectors.category EQ category) AND (Sectors.code EQ code)) unique()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); None
    }
  } 
  
  /**
   * @Note: This method can only be called after all secs have been selected and holded in Memory
   */
  private[model] def sectorToSecValidTimes = {
    val result = mutable.HashMap[String, mutable.ListBuffer[ValidTime[Sec]]]()
    
    val secsHolder = try {
      SELECT(Secs.*) FROM (Secs) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val sectorsHolder = try {
      SELECT(Sectors.*) FROM (Sectors) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val sectorSecs = try {
      SELECT (SectorSecs.*) FROM (SectorSecs) list()
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    for (sectorSec <- sectorSecs) {
      if (sectorSec.sec ne null) {
        val key = sectorSec.sector.key
        val validTime = ValidTime(sectorSec.sec, sectorSec.validFrom, sectorSec.validTo)
        val validTimes = result.get(key) match {
          case None => 
            val validTimes = mutable.ListBuffer[ValidTime[Sec]]()
            result += (key -> validTimes)
            validTimes
          case Some(x) => x
        }
      
        validTimes += validTime
      } else {
        log.warning("SectorSec: " + sectorSec + " has null sec. The id of this sectorSec is: " + SectorSecs.idOf(sectorSec))
      }
    }
    
    result
  }
}


