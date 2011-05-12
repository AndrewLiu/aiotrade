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
import org.aiotrade.lib.util.ValidTime
import ru.circumflex.orm.Table
import ru.circumflex.orm._

/**
 * fullcode is defined as two parts first part is caterogy (6 chars), second part is subcategory
 * 
 * @author Caoyuan Deng
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
  
  lazy val sectorToSecValidTimes: collection.Map[String, collection.Seq[ValidTime[Sec]]] = {
    Sectors.sectorToSecValidTimes
  }

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
  
  def secsOf(sector: Sector): Seq[Sec] = Sectors.secsOf(sector)
  def secsOf(key: String): Seq[Sec] = withKey(key) match {
    case Some(sector) => secsOf(sector)
    case None => Nil
  }
  
  def withKey(key: String) = Sectors.withKey(key)
  def withCategoryCode(category: String, code: String): Option[Sector] = Sectors.withCategoryCode(category, code)
  
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

class Sector {
  var category: String = ""
  var code: String = ""
  var name: String = ""
  
  var secs: List[Sec] = Nil
  
  lazy val key = category + "." + code
}

object Sectors extends Table[Sector] {
  val category = "category" VARCHAR(6) DEFAULT("''")
  val code = "code" VARCHAR(20) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)
  
  val categoryIdx = getClass.getSimpleName + "_category_idx" INDEX(category.name)
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)
  
  
  // --- helpers:
  
  private[model] def allSectors: Seq[String] = {
    SELECT (Sectors.*) FROM (Sectors) list() map (_.key)
  }
  
  private[model] def sectorsOf(category: String): Seq[Sector] = {
    SELECT (Sectors.*) FROM (Sectors) WHERE (Sectors.category EQ category) list()
  } 
  
  private[model] def secsOf(sector: Sector): Seq[Sec] = {
    SELECT (Secs.*) FROM (SectorSecs JOIN Secs) WHERE (SectorSecs.sector.field EQ Sectors.idOf(sector)) list()
  }
  
  private[model] def withKey(key: String): Option[Sector] = {
    val (category, code) = Sector.toCategoryCode(key)
    withCategoryCode(category, code)
  }
  
  private[model] def withCategoryCode(category: String, code: String): Option[Sector] = {
    SELECT (Sectors.*) FROM (Sectors) WHERE ((Sectors.category EQ category) AND (Sectors.code EQ code)) unique()
  } 
  
  /**
   * @Note: This method can only be called after all secs have been selected and holded in Memory
   */
  private[model] def sectorToSecValidTimes = {
    val result = mutable.HashMap[String, mutable.ListBuffer[ValidTime[Sec]]]()
    
    val sectorsHolder = SELECT(Sectors.*) FROM (Sectors) list()
    val sectorSecs = SELECT (SectorSecs.*) FROM (SectorSecs) list()
    for (sectorSec <- sectorSecs) {
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
    }
    
    result
  }
}


