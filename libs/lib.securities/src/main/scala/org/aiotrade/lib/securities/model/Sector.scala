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

import scala.collection.immutable
import ru.circumflex.orm.Table

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


