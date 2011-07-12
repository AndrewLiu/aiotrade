package org.aiotrade.lib.securities.model

import java.util.logging.Logger
import net.lag.configgy.Config
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.json.JsonOutputStreamWriter
import org.aiotrade.lib.json.JsonSerializable
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable
import java.io.IOException

object MoneyFlows1d extends MoneyFlows {
  override def config = org.aiotrade.lib.util.config.Config()
  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  @deprecated
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

  @deprecated
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
  override def config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)
  val loadDaysInMilsec = config.getInt("dataserver.loadDaysOfSer1m", 5) * 1000*60*60*24

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  @deprecated
  def minuteMoneyFlowOf(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    if (isServer) minuteMoneyFlowOf_nocached(sec, minuteRoundedTime) else minuteMoneyFlowOf_cached(sec, minuteRoundedTime)
  }

  /**
   * @Note do not use it when table is partitioned on secs_id, since this qeury is only on time
   */
  @deprecated
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

  @deprecated
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

  override def moneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND
      (this.time GE System.currentTimeMillis - loadDaysInMilsec)
    ) ORDER_BY (this.time) list
  }
}

abstract class MoneyFlows extends Table[MoneyFlow] {
  val log = Logger.getLogger(this.getClass.getName)
  def config: Config
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

    try{
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
    } catch {
      case e => log.severe("Failed to saveBatch moneyflow due to " + e.getMessage)
    }
  }

  def saveBatch(atSameTime: Long, mfs: ArrayList[MoneyFlow]) {
    if (mfs.isEmpty) return
    val exists = mutable.Map[Sec, MoneyFlow]()
    (SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime)
      ) list()
    ) foreach {x => exists.put(x.sec, x)}
    val updates = new ArrayList[MoneyFlow]()
    val inserts = new ArrayList[MoneyFlow]()
    var i = -1
    while ({i += 1; i < mfs.length}) {
      val quote = mfs(i)
      exists.get(quote.sec) match {
        case Some(existOne) =>
          existOne.copyFrom(quote)
          updates += existOne
        case None =>
          inserts += quote
      }
    }
    if (updates.length > 0) {
      this.updateBatch_!(updates.toArray)
    }
    if (inserts.length > 0) {
      this.insertBatch_!(inserts.toArray)
    }
  }
}


/**
 * The definition of "super/large/small block" will depond on amount
 */
@serializable
class MoneyFlow extends TVal with Flag with JsonSerializable {
  @transient var _sec: Sec = _
  def sec = _sec
  def sec_=(sec: Sec) {
    _uniSymbol = sec.uniSymbol
    _sec = sec
  }
  
  private var _uniSymbol: String = _
  def uniSymbol = _uniSymbol

  private val data = new Array[Double](24)
  
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

  def smallVolumeIn = data(18)
  def smallAmountIn = data(19)
  def smallVolumeOut = data(20)
  def smallAmountOut = data(21)
  def smallVolumeEven = data(22)
  def smallAmountEven = data(23)

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

  def smallVolumeIn_=(v: Double) {data(18) = v}
  def smallAmountIn_=(v: Double) {data(19) = v}
  def smallVolumeOut_=(v: Double) {data(20) = v}
  def smallAmountOut_=(v: Double) {data(21) = v}
  def smallVolumeEven_=(v: Double) {data(22) = v}
  def smallAmountEven_=(v: Double) {data(23) = v}
  
  // --- no db fields
  def totalVolume: Double = totalVolumeIn - totalVolumeOut
  def totalAmount: Double = totalAmountIn - totalAmountOut
  def superVolume: Double = superVolumeIn - superVolumeOut
  def superAmount: Double = superAmountIn - superAmountOut
  def largeVolume: Double = largeVolumeIn - largeVolumeOut
  def largeAmount: Double = largeAmountIn - largeAmountOut
  def smallVolume: Double = smallVolumeIn - smallVolumeOut
  def smallAmount: Double = smallAmountIn - smallAmountOut
  
  var isTransient = true

  def copyFrom(another: MoneyFlow) {
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  @throws(classOf[IOException])
  def writeJson(out: JsonOutputStreamWriter) {
    out.write("s", _uniSymbol)
    out.write(',')
    out.write("t", time)
    out.write(',')
    out.write("v", data)
  }

  @throws(classOf[IOException])
  def readJson(fields: collection.Map[String, _]) {
    _uniSymbol  = fields("s").asInstanceOf[String]
    time    = fields("t").asInstanceOf[Long] * 1000
    var vs  = fields("v").asInstanceOf[List[Number]]
    var i = 0
    while (!vs.isEmpty) {
      data(i) = vs.head.doubleValue
      vs = vs.tail
      i += 1
    }
  }
}
