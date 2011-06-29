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

  ///////////////////////////////////////////////////////////////////////////////////
  // @TODO Change the DB fields
  // /////////////////////////////////////////////////////////////////
  val middleVolumeIn = ""DOUBLE()//"middleVolumeIn" DOUBLE()
  val middleAmountIn = ""DOUBLE()//"middleAmountIn" DOUBLE()
  val middleVolumeOut = ""DOUBLE()//"middleVolumeOut" DOUBLE()
  val middleAmountOut = ""DOUBLE()//"middleAmountOut" DOUBLE()
  val middleVolumeEven = ""DOUBLE()//"middleVolumeEven" DOUBLE()
  val middleAmountEven = ""DOUBLE()//"middleAmountEven" DOUBLE()

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


/**
 * The definition of "super/large/middle/small block" will depond on amount
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

  var amountInCount = 0
  var amountOutCount = 0

  /**
   * Weighted average relative amount.
   */
  var relativeAmount = 0.0

  private val data = new Array[Double](30)
  
  def totalVolumeIn = data(0)
  def totalAmountIn = data(1)
  def totalVolumeOut = data(2)
  def totalAmountOut = data(3)
  def totalVolumeEven = data(4)
  def totalAmountEven = data(5)

  def superVolumeIn = data(6)
  def superAmountIn = data(7)
  def superVolumeOut = data(8)
  def superAmountOut = data(9)
  def superVolumeEven = data(10)
  def superAmountEven = data(11)

  def largeVolumeIn = data(12)
  def largeAmountIn = data(13)
  def largeVolumeOut = data(14)
  def largeAmountOut = data(15)
  def largeVolumeEven = data(16)
  def largeAmountEven = data(17)

  def middleVolumeIn = data(18)
  def middleAmountIn = data(19)
  def middleVolumeOut = data(20)
  def middleAmountOut = data(21)
  def middleVolumeEven = data(22)
  def middleAmountEven = data(23)

  def smallVolumeIn = data(24)
  def smallAmountIn = data(25)
  def smallVolumeOut = data(26)
  def smallAmountOut = data(27)
  def smallVolumeEven = data(28)
  def smallAmountEven = data(29)

  def totalVolumeIn_=(v: Double) {data(0) = v}
  def totalAmountIn_=(v: Double) {data(1) = v}
  def totalVolumeOut_=(v: Double) {data(2) = v}
  def totalAmountOut_=(v: Double) {data(3) = v}
  def totalVolumeEven_=(v: Double) {data(4) = v}
  def totalAmountEven_=(v: Double) {data(5) = v}

  def superVolumeIn_=(v: Double) {data(6) = v}
  def superAmountIn_=(v: Double) {data(7) = v}
  def superVolumeOut_=(v: Double) {data(8) = v}
  def superAmountOut_=(v: Double) {data(9) = v}
  def superVolumeEven_=(v: Double) {data(10) = v}
  def superAmountEven_=(v: Double) {data(11) = v}

  def largeVolumeIn_=(v: Double) {data(12) = v}
  def largeAmountIn_=(v: Double) {data(13) = v}
  def largeVolumeOut_=(v: Double) {data(14) = v}
  def largeAmountOut_=(v: Double) {data(15) = v}
  def largeVolumeEven_=(v: Double) {data(16) = v}
  def largeAmountEven_=(v: Double) {data(17) = v}

  def middleVolumeIn_=(v: Double) {data(18) = v}
  def middleAmountIn_=(v: Double) {data(19) = v}
  def middleVolumeOut_=(v: Double) {data(20) = v}
  def middleAmountOut_=(v: Double) {data(21) = v}
  def middleVolumeEven_=(v: Double) {data(22) = v}
  def middleAmountEven_=(v: Double) {data(23) = v}

  def smallVolumeIn_=(v: Double) {data(24) = v}
  def smallAmountIn_=(v: Double) {data(25) = v}
  def smallVolumeOut_=(v: Double) {data(26) = v}
  def smallAmountOut_=(v: Double) {data(27) = v}
  def smallVolumeEven_=(v: Double) {data(28) = v}
  def smallAmountEven_=(v: Double) {data(29) = v}
  
  // --- no db fields
  def totalVolume: Double = totalVolumeIn - totalVolumeOut
  def totalAmount: Double = totalAmountIn - totalAmountOut
  def superVolume: Double = superVolumeIn - superVolumeOut
  def superAmount: Double = superAmountIn - superAmountOut
  def largeVolume: Double = largeVolumeIn - largeVolumeOut
  def largeAmount: Double = largeAmountIn - largeAmountOut
  def middleVolume: Double = middleVolumeIn - middleVolumeOut
  def middleAmount: Double = middleAmountIn - middleAmountOut
  def smallVolume: Double = smallVolumeIn - smallVolumeOut
  def smallAmount: Double = smallAmountIn - smallAmountOut
  
  var isTransient = true

  def copyFrom(another: MoneyFlow) {
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

}
