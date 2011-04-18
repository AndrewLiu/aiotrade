package org.aiotrade.lib.securities.model

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable

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

abstract class MoneyFlows extends Table[MoneyFlow] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val totalVolumeIn = "totalVolumeIn" DOUBLE()
  val totalAmountIn = "totalAmountIn" DOUBLE()
  val totalVolumeOut = "totalVolumeOut" DOUBLE()
  val totalAmountOut = "totalAmountOut" DOUBLE()
  val totalVolumeEven = "totalVolumeEven" DOUBLE()
  val totalAmountEven = "totalAmountEven" DOUBLE()

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

  val smallVolumeIn = "smallVolume" DOUBLE()
  val smallAmountIn = "smallAmount" DOUBLE()
  val smallVolumeOut = "smallVolume" DOUBLE()
  val smallAmountOut = "smallAmount" DOUBLE()
  val smallVolumeEven = "smallVolume" DOUBLE()
  val smallAmountEven = "smallAmount" DOUBLE()
  
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
}


/**
 * The definition of "super/large/small block" will depond on amount
 */
class MoneyFlow extends TVal with Flag {
  var sec: Sec = _
  
  var totalVolumeIn: Double = _
  var totalAmountIn: Double = _
  var totalVolumeOut: Double = _
  var totalAmountOut: Double = _
  var totalVolumeEven: Double = _
  var totalAmountEven: Double = _

  var superVolumeIn: Double = _
  var superAmountIn: Double = _
  var superVolumeOut: Double = _
  var superAmountOut: Double = _
  var superVolumeEven: Double = _
  var superAmountEven: Double = _

  var largeVolumeIn: Double = _
  var largeAmountIn: Double = _
  var largeVolumeOut: Double = _
  var largeAmountOut: Double = _
  var largeVolumeEven: Double = _
  var largeAmountEven: Double = _

  var smallVolumeIn: Double = _
  var smallAmountIn: Double = _
  var smallVolumeOut: Double = _
  var smallAmountOut: Double = _
  var smallVolumeEven: Double = _
  var smallAmountEven: Double = _

  // --- no db fields
  var isTransient = true

  def totalVolume: Double = totalVolumeIn - totalVolumeOut
  def totalAmount: Double = totalAmountIn - totalAmountOut
  def superVolume: Double = superVolumeIn - superVolumeOut
  def superAmount: Double = superAmountIn - superAmountOut
  def largeVolume: Double = largeVolumeIn - largeVolumeOut
  def largeAmount: Double = largeAmountIn - largeAmountOut
  def smallVolume: Double = smallVolumeIn - smallVolumeOut
  def smallAmount: Double = smallAmountIn - smallAmountOut
}
