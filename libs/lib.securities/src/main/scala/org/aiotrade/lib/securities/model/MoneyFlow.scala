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

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable



/**
 * The definition of "super/large/small block" will depond on amount
 */
@serializable
class MoneyFlow extends BelongsToSec with TVal with Flag {

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag 
  def flag_=(flag: Int) {
    this._flag = flag
  }

  private val data = new Array[Double](24)
  
  def superVolumeIn = data(0)
  def superAmountIn = data(1)
  def superVolumeOut = data(2)
  def superAmountOut = data(3)
  def superVolumeEven = data(4)
  def superAmountEven = data(5)

  def largeVolumeIn = data(6)
  def largeAmountIn = data(7)
  def largeVolumeOut = data(8)
  def largeAmountOut = data(9)
  def largeVolumeEven = data(10)
  def largeAmountEven = data(11)

  def mediumVolumeIn = data(12)
  def mediumAmountIn = data(13)
  def mediumVolumeOut = data(14)
  def mediumAmountOut = data(15)
  def mediumVolumeEven = data(16)
  def mediumAmountEven = data(17)

  def smallVolumeIn = data(18)
  def smallAmountIn = data(19)
  def smallVolumeOut = data(20)
  def smallAmountOut = data(21)
  def smallVolumeEven = data(22)
  def smallAmountEven = data(23)

  def superVolumeIn_=(v: Double) {data(0) = v}
  def superAmountIn_=(v: Double) {data(1) = v}
  def superVolumeOut_=(v: Double) {data(2) = v}
  def superAmountOut_=(v: Double) {data(3) = v}
  def superVolumeEven_=(v: Double) {data(4) = v}
  def superAmountEven_=(v: Double) {data(5) = v}

  def largeVolumeIn_=(v: Double) {data(6) = v}
  def largeAmountIn_=(v: Double) {data(7) = v}
  def largeVolumeOut_=(v: Double) {data(8) = v}
  def largeAmountOut_=(v: Double) {data(9) = v}
  def largeVolumeEven_=(v: Double) {data(10) = v}
  def largeAmountEven_=(v: Double) {data(11) = v}

  def mediumVolumeIn_=(v: Double) {data(12) = v}
  def mediumAmountIn_=(v: Double) {data(13) = v}
  def mediumVolumeOut_=(v: Double) {data(14) = v}
  def mediumAmountOut_=(v: Double) {data(15) = v}
  def mediumVolumeEven_=(v: Double) {data(16) = v}
  def mediumAmountEven_=(v: Double) {data(17) = v}

  def smallVolumeIn_=(v: Double) {data(18) = v}
  def smallAmountIn_=(v: Double) {data(19) = v}
  def smallVolumeOut_=(v: Double) {data(20) = v}
  def smallAmountOut_=(v: Double) {data(21) = v}
  def smallVolumeEven_=(v: Double) {data(22) = v}
  def smallAmountEven_=(v: Double) {data(23) = v}
  
  // --- no db fields
  
  // sum
  def volumeIn = superVolumeIn + largeVolumeIn + mediumVolumeIn + smallVolumeIn
  def amountIn = superAmountIn + largeAmountIn + mediumAmountIn + smallAmountIn
  def volumeOut = superVolumeOut + largeVolumeOut + mediumVolumeOut + smallVolumeOut
  def amountOut = superAmountOut + largeAmountOut + mediumAmountOut + smallAmountOut
  def volumeEven = superVolumeEven + largeVolumeEven + mediumVolumeEven + smallVolumeEven
  def amountEven = superAmountEven + largeAmountEven + mediumAmountEven + smallAmountEven

  // net sum
  def volumeNet = volumeIn + volumeOut
  def amountNet = amountIn + amountOut
  
  // net super
  def superVolumeNet = superVolumeIn + superVolumeOut
  def superAmountNet = superAmountIn + superAmountOut
  
  // net large
  def largeVolumeNet = largeVolumeIn + largeVolumeOut
  def largeAmountNet = largeAmountIn + largeAmountOut
  
  // net meduam
  def mediumVolumeNet = mediumVolumeIn + mediumVolumeOut
  def mediumAmountNet = mediumAmountIn + mediumAmountOut
  
  // net small
  def smallVolumeNet = smallVolumeIn + smallVolumeOut
  def smallAmountNet = smallAmountIn + smallAmountOut
  
  var isTransient = true

  def copyFrom(another: MoneyFlow) {
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

}

abstract class MoneyFlows extends Table[MoneyFlow] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val superVolumeIn = "superVolumeIn" DOUBLE()
  val superAmountIn = "superAmountIn" DOUBLE()
  val superVolumeOut = "superVolumeOut" DOUBLE()
  val superAmountOut = "superAmountOut" DOUBLE()
  val superVolumeEven = "superVolumeEven" DOUBLE()
  val superAmountEven = "superAmountEven" DOUBLE()

  val largeVolumeIn = "largeVolumeIn" DOUBLE()
  val largeAmountIn = "largeAmountIn" DOUBLE()
  val largeVolumeOut = "largeVolumeOut" DOUBLE()
  val largeAmountOut = "largeAmountOut" DOUBLE()
  val largeVolumeEven = "largeVolumeEven" DOUBLE()
  val largeAmountEven = "largeAmountEven" DOUBLE()

  val mediumVolumeIn = "mediumVolumeIn" DOUBLE()
  val mediumAmountIn = "mediumAmountIn" DOUBLE()
  val mediumVolumeOut = "mediumVolumeOut" DOUBLE()
  val mediumAmountOut = "mediumAmountOut" DOUBLE()
  val mediumVolumeEven = "mediumVolumeEven" DOUBLE()
  val mediumAmountEven = "mediumAmountEven" DOUBLE()

  val smallVolumeIn = "smallVolumeIn" DOUBLE()
  val smallAmountIn = "smallAmountIn" DOUBLE()
  val smallVolumeOut = "smallVolumeOut" DOUBLE()
  val smallAmountOut = "smallAmountOut" DOUBLE()
  val smallVolumeEven = "smallVolumeEven" DOUBLE()
  val smallAmountEven = "smallAmountEven" DOUBLE()
  
  val flag = "flag" INTEGER()

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  def moneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    SELECT (this.*) FROM (this) WHERE (
      this.sec.field EQ Secs.idOf(sec)
    ) ORDER_BY (this.time) list
  }

  def closedMoneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    val xs = new ArrayList[MoneyFlow]()
    for (x <- moneyFlowOf(sec) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedMoneyFlowOf__filterByDB(sec: Sec): Seq[MoneyFlow] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
    ) ORDER_BY (this.time) list
  }
  
  def saveBatch(sec: Sec, sortedMfs: Seq[MoneyFlow]) {
    if (sortedMfs.isEmpty) return

    val head = sortedMfs.head
    val last = sortedMfs.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, MoneyFlow]()
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    ) foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedMfs.partition(x => exists.contains(x.time))
    for (x <- updates) {
      val existOne = exists(x.time)
      existOne.copyFrom(x)
      this.update_!(existOne)
    }

    this.insertBatch_!(inserts.toArray)
  }
}

// --- table
object MoneyFlows1d extends MoneyFlows {
  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  def dailyMoneyFlowOf(sec: Sec, dailyRoundedTime: Long): MoneyFlow = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = mutable.Map[Sec, MoneyFlow]()
        dailyCache.put(dailyRoundedTime, map)

        (SELECT (this.*) FROM (this) WHERE (
            (this.time EQ dailyRoundedTime)
          ) list
        ) foreach {x => map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.DAILY, newone)
        newone
    }
  }

  def dailyMoneyFlowOf_ignoreCache(sec: Sec, dailyRoundedTime: Long): MoneyFlow = synchronized {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ dailyRoundedTime)
      ) list
    ) headOption match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.DAILY, newone)
        newone
    }
  }
}

object MoneyFlows1m extends MoneyFlows {
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  def minuteMoneyFlowOf(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    if (isServer) minuteMoneyFlowOf_nocached(sec, minuteRoundedTime) else minuteMoneyFlowOf_cached(sec, minuteRoundedTime)
  }

  /**
   * @Note do not use it when table is partitioned on secs_id, since this qeury is only on time
   */
  def minuteMoneyFlowOf_cached(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = mutable.Map[Sec, MoneyFlow]()
        minuteCache.put(minuteRoundedTime, map)

        (SELECT (this.*) FROM (this) WHERE (
            (this.time EQ minuteRoundedTime)
          ) list
        ) foreach {x => map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.ONE_MIN, newone)
        newone
    }
  }

  def minuteMoneyFlowOf_nocached(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    ) headOption match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.ONE_MIN, newone)
        newone
    }
  }
}
