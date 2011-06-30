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

import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
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

  val volumeIn = TVar[Double]("Vi", Plot.None)
  val amountIn = TVar[Double]("Ai", Plot.None)
  val volumeOut = TVar[Double]("Vo", Plot.None)
  val amountOut = TVar[Double]("Ao", Plot.None)
  val volumeEven = TVar[Double]("Ve", Plot.None)
  val amountEven = TVar[Double]("Ae", Plot.None)
  
  val superVolumeIn = TVar[Double]("suVi", Plot.None)
  val superAmountIn = TVar[Double]("suAi", Plot.None)
  val superVolumeOut = TVar[Double]("suVo", Plot.None)
  val superAmountOut = TVar[Double]("suAo", Plot.None)
  val superVolumeEven = TVar[Double]("suVe", Plot.None)
  val superAmountEven = TVar[Double]("suAe", Plot.None)

  val largeVolumeIn = TVar[Double]("laVi", Plot.None)
  val largeAmountIn = TVar[Double]("laAi", Plot.None)
  val largeVolumeOut = TVar[Double]("laVo", Plot.None)
  val largeAmountOut = TVar[Double]("laAo", Plot.None)
  val largeVolumeEven = TVar[Double]("laVe", Plot.None)
  val largeAmountEven = TVar[Double]("laAe", Plot.None)

  val mediumVolumeIn = TVar[Double]("meVi", Plot.None)
  val mediumAmountIn = TVar[Double]("meAi", Plot.None)
  val mediumVolumeOut = TVar[Double]("meVo", Plot.None)
  val mediumAmountOut = TVar[Double]("meAo", Plot.None)
  val mediumVolumeEven = TVar[Double]("meVe", Plot.None)
  val mediumAmountEven = TVar[Double]("meAe", Plot.None)

  val smallVolumeIn = TVar[Double]("smVi", Plot.None)
  val smallAmountIn = TVar[Double]("smAi", Plot.None)
  val smallVolumeOut = TVar[Double]("smVo", Plot.None)
  val smallAmountOut = TVar[Double]("smAo", Plot.None)
  val smallVolumeEven = TVar[Double]("smVe", Plot.None)
  val smallAmountEven = TVar[Double]("smAe", Plot.None)
  
  val volumeNet = TVar[Double]("V", Plot.None)
  val amountNet = TVar[Double]("A", Plot.None)
  val superVolumeNet  = TVar[Double]("suV", Plot.None)
  val superAmountNet  = TVar[Double]("suA", Plot.None)
  val largeVolumeNet  = TVar[Double]("laV", Plot.None)
  val largeAmountNet  = TVar[Double]("laA", Plot.None)
  val mediumVolumeNet = TVar[Double]("meV", Plot.None)
  val mediumAmountNet = TVar[Double]("meA", Plot.None)
  val smallVolumeNet  = TVar[Double]("smV", Plot.None)
  val smallAmountNet  = TVar[Double]("smA", Plot.None)
  
  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case mf: MoneyFlow =>
        relativeAmount(time) = mf.relativeAmount
        amountInCount(time) = mf.amountInCount
        amountOutCount(time) = mf.amountOutCount
	
        volumeIn(time) = mf.volumeIn
        amountIn(time) = mf.amountIn
        volumeOut(time) = mf.volumeOut
        amountOut(time) = mf.amountOut
        volumeEven(time) = mf.volumeEven
        amountEven(time) = mf.amountEven
        
        superVolumeIn(time) = mf.superVolumeIn
        superAmountIn(time) = mf.superAmountIn
        superVolumeOut(time) = mf.superVolumeOut
        superAmountOut(time) = mf.superAmountOut
        superVolumeEven(time) = mf.superVolumeEven
        superAmountEven(time) = mf.superAmountEven

        largeVolumeIn(time) = mf.largeVolumeIn
        largeAmountIn(time) = mf.largeAmountIn
        largeVolumeOut(time) = mf.largeVolumeOut
        largeAmountOut(time) = mf.largeAmountOut
        largeVolumeEven(time) = mf.largeVolumeEven
        largeAmountEven(time) = mf.largeAmountEven

        mediumVolumeIn(time) = mf.mediumVolumeIn
        mediumAmountIn(time) = mf.mediumAmountIn
        mediumVolumeOut(time) = mf.mediumVolumeOut
        mediumAmountOut(time) = mf.mediumAmountOut
        mediumVolumeEven(time) = mf.mediumVolumeEven
        mediumAmountEven(time) = mf.mediumAmountEven

        smallVolumeIn(time) = mf.smallVolumeIn
        smallAmountIn(time) = mf.smallAmountIn
        smallVolumeOut(time) = mf.smallVolumeOut
        smallAmountOut(time) = mf.smallAmountOut
        smallVolumeEven(time) = mf.smallVolumeEven
        smallAmountEven(time) = mf.smallAmountEven
        
        volumeNet(time) = mf.volumeNet
        amountNet(time) = mf.amountNet
        superVolumeNet(time) = mf.superVolumeNet
        superAmountNet(time) = mf.superAmountNet
        largeVolumeNet(time) = mf.largeVolumeNet
        largeAmountNet(time) = mf.largeAmountNet
        mediumVolumeNet(time) = mf.mediumVolumeNet
        mediumAmountNet(time) = mf.mediumAmountNet
        smallVolumeNet(time) = mf.smallVolumeNet
        smallAmountNet(time) = mf.smallAmountNet
      case _ =>
    }
  }

  def valueOf(time: Long): Option[MoneyFlow] = {
    if (exists(time)) {
      val mf = new MoneyFlow

      mf.relativeAmount = relativeAmount(time)
      mf.amountInCount = amountInCount(time)
      mf.amountOutCount = amountOutCount(time)

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

      mf.mediumVolumeIn = mediumVolumeIn(time)
      mf.mediumAmountIn = mediumAmountIn(time)
      mf.mediumVolumeOut = mediumVolumeOut(time)
      mf.mediumAmountOut = mediumAmountOut(time)
      mf.mediumAmountEven = mediumVolumeEven(time)
      mf.mediumVolumeEven = mediumAmountEven(time)

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
    
    assignValue(mf)
        
    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }

  override def shortName =  _shortName
  override def shortName_=(name: String) {
    this._shortName = name
  }
    
}

object MoneyFlowSer {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(vmap: collection.Map[String, Array[_]]): Array[MoneyFlow] = {
    val mfs = new ArrayList[MoneyFlow]()
    try {
      val times = vmap(".")
      val volumeIns = vmap("Vi")
      val amountIns = vmap("Ai")
      val volumeOuts = vmap("Vo")
      val amountOuts = vmap("Ao")
      val volumeEvens = vmap("Ve")
      val amountEvens = vmap("Ae")
  
      val superVolumeIns = vmap("suVi")
      val superAmountIns = vmap("suAi")
      val superVolumeOuts = vmap("suVo")
      val superAmountOuts = vmap("suAo")
      val superVolumeEvens = vmap("suVe")
      val superAmountEvens = vmap("suAe")

      val largeVolumeIns = vmap("laVi")
      val largeAmountIns = vmap("laAi")
      val largeVolumeOuts = vmap("laVo")
      val largeAmountOuts = vmap("laAo")
      val largeVolumeEvens = vmap("laVe")
      val largeAmountEvens = vmap("laAe")

      val mediumVolumeIns = vmap("meVi")
      val mediumAmountIns = vmap("meAi")
      val mediumVolumeOuts = vmap("meVo")
      val mediumAmountOuts = vmap("meAo")
      val mediumVolumeEvens = vmap("meVe")
      val mediumAmountEvens = vmap("meAe")

      val smallVolumeIns = vmap("smVi")
      val smallAmountIns = vmap("smAi")
      val smallVolumeOuts = vmap("smVo")
      val smallAmountOuts = vmap("smAo")
      val smallVolumeEvens = vmap("smVe")
      val smallAmountEvens = vmap("smAe")

      val volumeNets = vmap("V")
      val amountNets = vmap("A")
      val superVolumeNets = vmap("suV")
      val superAmountNets = vmap("suA")
      val largeVolumeNets = vmap("laV")
      val largeAmountNets = vmap("laA")
      val mediumVolumeNets = vmap("meV")
      val mediumAmountNets = vmap("meA")
      val smallVolumeNets = vmap("smV")
      val smallAmountNets = vmap("smA")
      
      var i = -1
      while ({i += 1; i < times.length}) {
        // the time should be properly set to 00:00 of exchange location's local time, i.e. rounded to TFreq.DAILY
        val time = times(i).asInstanceOf[Long]
        val mf = new MoneyFlow

        mf.time = time
        
        mf.superVolumeIn = superVolumeIns(i).asInstanceOf[Double]
        mf.superAmountIn = superAmountIns(i).asInstanceOf[Double]
        mf.superVolumeOut = superVolumeOuts(i).asInstanceOf[Double]
        mf.superAmountOut = superAmountOuts(i).asInstanceOf[Double]
        mf.superVolumeEven = superVolumeEvens(i).asInstanceOf[Double]
        mf.superAmountEven = superAmountEvens(i).asInstanceOf[Double]

        mf.largeVolumeIn = largeVolumeIns(i).asInstanceOf[Double]
        mf.largeAmountIn = largeAmountIns(i).asInstanceOf[Double]
        mf.largeVolumeOut = largeVolumeOuts(i).asInstanceOf[Double]
        mf.largeAmountOut = largeAmountOuts(i).asInstanceOf[Double]
        mf.largeVolumeEven = largeVolumeEvens(i).asInstanceOf[Double]
        mf.largeAmountEven = largeAmountEvens(i).asInstanceOf[Double]

        mf.mediumVolumeIn = mediumVolumeIns(i).asInstanceOf[Double]
        mf.mediumAmountIn = mediumAmountIns(i).asInstanceOf[Double]
        mf.mediumVolumeOut = mediumVolumeOuts(i).asInstanceOf[Double]
        mf.mediumAmountOut = mediumAmountOuts(i).asInstanceOf[Double]
        mf.mediumVolumeEven = mediumVolumeEvens(i).asInstanceOf[Double]
        mf.mediumAmountEven = mediumAmountEvens(i).asInstanceOf[Double]

        mf.smallVolumeIn = smallVolumeIns(i).asInstanceOf[Double]
        mf.smallAmountIn = smallAmountIns(i).asInstanceOf[Double]
        mf.smallVolumeOut = smallVolumeOuts(i).asInstanceOf[Double]
        mf.smallAmountOut = smallAmountOuts(i).asInstanceOf[Double]
        mf.smallVolumeEven = smallVolumeEvens(i).asInstanceOf[Double]
        mf.smallAmountEven = smallAmountEvens(i).asInstanceOf[Double]

        mfs += mf
      }
    } catch {
      case ex => log.warning(ex.getMessage)
    }

    mfs.toArray
  }
}




