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

import ru.circumflex.orm.Table

class SecDividend extends BelongsToSec {
  
  var prevClose: Double = _
  var adjWeight: Double = _
  var adjOffset: Double = _
  var cashBonus: Double = _
  var shareBonus: Double = _ // bonus issue, entitle bonus share
  var shareRight: Double = _ // allotment of shares in sharePrice
  var shareRightPrice: Double = _ // price of allotment of share
  var registerDate: Long = _
  var dividendDate: Long = _
  
  final 
  def dividedClose = accurateAdjust(prevClose)
  
  /**
   * p' = (p - offset) / weight 
   * weight = (p - offset) / p'
   * offset = p - p' * weight
   */
  final 
  def setAdjParams(p1: Double, p1Adj: Double, p2: Double, p2Adj: Double) {
    adjWeight = (p1 - p2) / (p1Adj - p2Adj)
    adjOffset = p1 - p1Adj * adjWeight
  }
  
  private def cashAfterward = cashBonus - shareRight * shareRightPrice  // adjWeight
  private def shareAfterward = 1 + shareRight + shareBonus              // adjOffset

  final 
  def accurateAdjust(price: Double) = (price - cashAfterward) / shareAfterward
  final 
  def accurateUnadjust(price: Double) = price * shareAfterward + cashAfterward
  
  final 
  def adjust(price: Double) = {
    val p = accurateAdjust(price)
    if (p != price) p else (if (adjWeight != 0) (price - adjOffset) / adjWeight else price)
  }
  
  final 
  def unadjust(price: Double) = {
    val p = accurateUnadjust(price)
    if (p != price) p else (if (adjWeight != 0) price * adjWeight + adjOffset else price)
  }
  
  final 
  def forwardAdjust(price: Double) = adjust(price)
  final 
  def backwradAdjust(price: Double) = unadjust(price)
  
  def copyFrom(another: SecDividend) {
    this.cashBonus = another.cashBonus
    this.dividendDate = another.dividendDate
    this.registerDate = another.registerDate
    this.shareBonus = another.shareBonus
    this.shareRightPrice = another.shareRightPrice
    this.shareRight = another.shareRight
  }
  
  override 
  def equals(a: Any): Boolean = a match {
    case x: SecDividend =>
      this.sec == x.sec && 
      this.dividendDate == x.dividendDate && 
      equals(this.cashBonus, x.cashBonus) && 
      equals(this.adjWeight, x.adjWeight) &&
      equals(this.shareBonus - x.shareBonus) && 
      equals(this.shareRightPrice - x.shareRightPrice) &&
      equals(this.shareRight - x.shareRight)
    case _ => false
  }
  
  private def equals(a: Double, b: Double) = math.abs(a - b) < 1e-6
}

object SecDividends extends Table[SecDividend] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val prevClose = "prevClose" DOUBLE()
  val adjWeight = "adjWeight" DOUBLE()
  val adjOffset = "adjOffset" DOUBLE()
  val cashBonus = "cashBonus" DOUBLE()
  val shareBonus = "shareBonus" DOUBLE()
  val shareRight = "shareRight" DOUBLE()
  val shareRightPrice = "shareRightPrice" DOUBLE()
  val registerDate = "registerDate" BIGINT()
  val dividendDate = "dividendDate" BIGINT()
}

