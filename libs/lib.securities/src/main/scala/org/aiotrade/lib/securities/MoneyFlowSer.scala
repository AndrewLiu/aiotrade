/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{TVal, TSerEvent, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.Sec

/**
 *
 * @author Caoyuan Deng
 */
class MoneyFlowSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {

  private var _shortName: String = ""
  
  val amountInCount = TVar[Int]("aIC", Plot.None)
  val amountOutCount = TVar[Int]("aOC", Plot.None)

  val relativeAmount = TVar[Double]("RA", Plot.None)

  val totalVolume = TVar[Double]("TV", Plot.None)
  val totalAmount = TVar[Double]("TA", Plot.None)
  val totalVolumeIn = TVar[Double]("TVi", Plot.None)
  val totalAmountIn = TVar[Double]("TAi", Plot.None)
  val totalVolumeOut = TVar[Double]("TVo", Plot.None)
  val totalAmountOut = TVar[Double]("TAo", Plot.None)
  val totalVolumeEven = TVar[Double]("TVe", Plot.None)
  val totalAmountEven = TVar[Double]("TAe", Plot.None)
  
  val superVolume = TVar[Double]("SV", Plot.None)
  val superAmount = TVar[Double]("SA", Plot.None)
  val superVolumeIn = TVar[Double]("SVi", Plot.None)
  val superAmountIn = TVar[Double]("SAi", Plot.None)
  val superVolumeOut = TVar[Double]("SVo", Plot.None)
  val superAmountOut = TVar[Double]("SAo", Plot.None)
  val superVolumeEven = TVar[Double]("SVe", Plot.None)
  val superAmountEven = TVar[Double]("SAe", Plot.None)

  val largeVolume = TVar[Double]("LV", Plot.None)
  val largeAmount = TVar[Double]("LA", Plot.None)
  val largeVolumeIn = TVar[Double]("LVi", Plot.None)
  val largeAmountIn = TVar[Double]("LAi", Plot.None)
  val largeVolumeOut = TVar[Double]("LVo", Plot.None)
  val largeAmountOut = TVar[Double]("LAo", Plot.None)
  val largeVolumeEven = TVar[Double]("LVe", Plot.None)
  val largeAmountEven = TVar[Double]("LAe", Plot.None)

  val middleVolume = TVar[Double]("MV", Plot.None)
  val middleAmount = TVar[Double]("MA", Plot.None)
  val middleVolumeIn = TVar[Double]("MVi", Plot.None)
  val middleAmountIn = TVar[Double]("MAi", Plot.None)
  val middleVolumeOut = TVar[Double]("MVo", Plot.None)
  val middleAmountOut = TVar[Double]("MAo", Plot.None)
  val middleVolumeEven = TVar[Double]("MVe", Plot.None)
  val middleAmountEven = TVar[Double]("MAe", Plot.None)

  val smallVolume = TVar[Double]("sV", Plot.None)
  val smallAmount = TVar[Double]("sA", Plot.None)
  val smallVolumeIn = TVar[Double]("sVi", Plot.None)
  val smallAmountIn = TVar[Double]("sAi", Plot.None)
  val smallVolumeOut = TVar[Double]("sVo", Plot.None)
  val smallAmountOut = TVar[Double]("sAo", Plot.None)
  val smallVolumeEven = TVar[Double]("sVe", Plot.None)
  val smallAmountEven = TVar[Double]("sAe", Plot.None)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case mf: MoneyFlow =>
        relativeAmount(time) = mf.relativeAmount
        amountInCount(time) = mf.amountInCount
        amountOutCount(time) = mf.amountOutCount

        totalVolume(time) = mf.totalVolume
        totalAmount(time) = mf.totalAmount
        totalVolumeIn(time) = mf.totalVolumeIn
        totalAmountIn(time) = mf.totalAmountIn
        totalVolumeOut(time) = mf.totalVolumeOut
        totalAmountOut(time) = mf.totalAmountOut
        totalVolumeEven(time) = mf.totalVolumeEven
        totalAmountEven(time) = mf.totalAmountEven
        
        superVolume(time) = mf.superVolume
        superAmount(time) = mf.superAmount
        superVolumeIn(time) = mf.superVolumeIn
        superAmountIn(time) = mf.superAmountIn
        superVolumeOut(time) = mf.superVolumeOut
        superAmountOut(time) = mf.superAmountOut
        superVolumeEven(time) = mf.superVolumeEven
        superAmountEven(time) = mf.superAmountEven

        largeVolume(time) = mf.largeVolume
        largeAmount(time) = mf.largeAmount
        largeVolumeIn(time) = mf.largeVolumeIn
        largeAmountIn(time) = mf.largeAmountIn
        largeVolumeOut(time) = mf.largeVolumeOut
        largeAmountOut(time) = mf.largeAmountOut
        largeVolumeEven(time) = mf.largeVolumeEven
        largeAmountEven(time) = mf.largeAmountEven

        middleVolume(time) = mf.middleVolume
        middleAmount(time) = mf.middleAmount
        middleVolumeIn(time) = mf.middleVolumeIn
        middleAmountIn(time) = mf.middleAmountIn
        middleVolumeOut(time) = mf.middleVolumeOut
        middleAmountOut(time) = mf.middleAmountOut
        middleVolumeEven(time) = mf.middleVolumeEven
        middleAmountEven(time) = mf.middleAmountEven

        smallVolume(time) = mf.smallVolume
        smallAmount(time) = mf.smallAmount
        smallVolumeIn(time) = mf.smallVolumeIn
        smallAmountIn(time) = mf.smallAmountIn
        smallVolumeOut(time) = mf.smallVolumeOut
        smallAmountOut(time) = mf.smallAmountOut
        smallVolumeEven(time) = mf.smallVolumeEven
        smallAmountEven(time) = mf.smallAmountEven
      case _ =>
    }
  }

  def valueOf(time: Long): Option[MoneyFlow] = {
    if (exists(time)) {
      val mf = new MoneyFlow
      mf.relativeAmount = relativeAmount(time)
      mf.amountInCount = amountInCount(time)
      mf.amountOutCount = amountOutCount(time)

      mf.totalVolumeIn = totalVolumeIn(time)
      mf.totalAmountIn = totalAmountIn(time)
      mf.totalVolumeOut = totalVolumeOut(time)
      mf.totalAmountOut = totalAmountOut(time)
      mf.totalVolumeEven = totalVolumeEven(time)
      mf.totalAmountEven = totalAmountEven(time)

      mf.superVolumeIn = superVolumeIn(time)
      mf.superAmountIn = superAmountIn(time)
      mf.superVolumeOut = superVolumeOut(time)
      mf.superAmountOut = superAmountOut(time)
      mf.superVolumeEven = superVolumeEven(time)
      mf.superAmountEven = superAmountEven(time)

      mf.largeAmountIn = largeVolumeIn(time)
      mf.largeAmountIn = largeAmountIn(time)
      mf.largeVolumeOut = largeVolumeOut(time)
      mf.largeAmountOut = largeAmountOut(time)
      mf.largeVolumeEven = largeVolumeOut(time)
      mf.largeAmountEven = largeAmountOut(time)

      mf.middleVolumeIn = middleVolumeIn(time)
      mf.middleAmountIn = middleAmountIn(time)
      mf.middleVolumeOut = middleVolumeOut(time)
      mf.middleAmountOut = middleAmountOut(time)
      mf.middleAmountEven = middleVolumeEven(time)
      mf.middleVolumeEven = middleAmountEven(time)

      mf.smallVolumeIn = smallVolumeIn(time)
      mf.smallAmountIn = smallAmountIn(time)
      mf.smallVolumeOut = smallVolumeOut(time)
      mf.smallAmountOut = smallAmountOut(time)
      mf.smallAmountEven = smallVolumeEven(time)
      mf.smallVolumeEven = smallAmountEven(time)
      
      Some(mf)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(mf: MoneyFlow) {
    val time = mf.time
    createOrClear(time)

    relativeAmount(time) = mf.relativeAmount
    amountInCount(time) = mf.amountInCount
    amountOutCount(time) = mf.amountOutCount
    
    totalVolume(time) = mf.totalVolume
    totalAmount(time) = mf.totalAmount
    totalVolumeIn(time) = mf.totalVolumeIn
    totalAmountIn(time) = mf.totalAmountIn
    totalVolumeOut(time) = mf.totalVolumeOut
    totalAmountOut(time) = mf.totalAmountOut
    totalVolumeEven(time) = mf.totalVolumeEven
    totalAmountEven(time) = mf.totalAmountEven
        
    superVolume(time) = mf.superVolume
    superAmount(time) = mf.superAmount
    superVolumeIn(time) = mf.superVolumeIn
    superAmountIn(time) = mf.superAmountIn
    superVolumeOut(time) = mf.superVolumeOut
    superAmountOut(time) = mf.superAmountOut
    superVolumeEven(time) = mf.superVolumeEven
    superAmountEven(time) = mf.superAmountEven

    largeVolume(time) = mf.largeVolume
    largeAmount(time) = mf.largeAmount
    largeVolumeIn(time) = mf.largeVolumeIn
    largeAmountIn(time) = mf.largeAmountIn
    largeVolumeOut(time) = mf.largeVolumeOut
    largeAmountOut(time) = mf.largeAmountOut
    largeVolumeEven(time) = mf.largeVolumeEven
    largeAmountEven(time) = mf.largeAmountEven

    middleVolume(time) = mf.middleVolume
    middleAmount(time) = mf.middleAmount
    middleVolumeIn(time) = mf.middleVolumeIn
    middleAmountIn(time) = mf.middleAmountIn
    middleVolumeOut(time) = mf.middleVolumeOut
    middleAmountOut(time) = mf.middleAmountOut
    middleVolumeEven(time) = mf.middleVolumeEven
    middleAmountEven(time) = mf.middleAmountEven

    smallVolume(time) = mf.smallVolume
    smallAmount(time) = mf.smallAmount
    smallVolumeIn(time) = mf.smallVolumeIn
    smallAmountIn(time) = mf.smallAmountIn
    smallVolumeOut(time) = mf.smallVolumeOut
    smallAmountOut(time) = mf.smallAmountOut
    smallVolumeEven(time) = mf.smallVolumeEven
    smallAmountEven(time) = mf.smallAmountEven
        
    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }

  /**
   * This function adjusts linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

  override def shortName =  _shortName
  override def shortName_=(name: String) {
    this._shortName = name
  }
    
}





