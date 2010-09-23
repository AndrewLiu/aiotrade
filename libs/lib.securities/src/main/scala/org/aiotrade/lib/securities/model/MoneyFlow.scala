package org.aiotrade.lib.securities.model

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

object MoneyFlows1d extends MoneyFlows {
  private val dailyCache = new HashMap[Long, HashMap[Sec, MoneyFlow]]

  def dailyMoneyFlowOf(sec: Sec, dailyRoundedTime: Long): MoneyFlow = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = new HashMap[Sec, MoneyFlow]
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

  private val minuteCache = new HashMap[Long, HashMap[Sec, MoneyFlow]]

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
        val map = new HashMap[Sec, MoneyFlow]
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
  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val totalVolume = "totalVolume" DOUBLE()
  val totalAmount = "totalAmount" DOUBLE()

  val superVolume = "superVolume" DOUBLE()
  val superAmount = "superAmount" DOUBLE()

  val largeVolume = "largeVolume" DOUBLE()
  val largeAmount = "largeAmount" DOUBLE()

  val smallVolume = "smallVolume" DOUBLE()
  val smallAmount = "smallAmount" DOUBLE()
  
  val flag = "flag" INTEGER

  INDEX(getClass.getSimpleName + "_time_idx", time.name)

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
  
  var totalVolume: Double = _
  var totalAmount: Double = _

  var superVolume: Double = _
  var superAmount: Double = _

  var largeVolume: Double = _
  var largeAmount: Double = _

  var smallVolume: Double = _
  var smallAmount: Double = _

  // --- no db fields
  var isTransient = true
}
