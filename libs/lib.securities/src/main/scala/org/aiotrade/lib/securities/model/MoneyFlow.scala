package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._

object MoneyFlows1d extends MoneyFlows {
  def dailyMoneyFlowOf(sec: Sec, time: Long): MoneyFlow = synchronized {
    val cal = Calendar.getInstance(sec.exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)

    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ rounded)
      ) unique
    ) match {
      case Some(one) => one
      case None =>
        val newone = new MoneyFlow
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_! // @todo when to close it and update to db?
        newone.justOpen_!
        MoneyFlows1d.save(newone)
        commit
        newone
    }
  }

}

object MoneyFlows1m extends MoneyFlows

abstract class MoneyFlows extends Table[MoneyFlow] {
  val sec = "secs_id" REFERENCES(Secs)

  val time = "time" BIGINT

  val totalVolume = "totalVolume" FLOAT(18, 2)
  val totalAmount = "totalAmount" FLOAT(18, 2)

  val superVolume = "superVolume" FLOAT(18, 2)
  val superAmount = "superAmount" FLOAT(18, 2)

  val largeVolume = "largeVolume" FLOAT(18, 2)
  val largeAmount = "largeAmount" FLOAT(18, 2)

  val smallVolume = "smallVolume" FLOAT(18, 2)
  val smallAmount = "smallAmount" FLOAT(18, 2)
  
  val flag = "flag" INTEGER

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
  
  var totalVolume: Float = _
  var totalAmount: Float = _

  var superVolume: Float = _
  var superAmount: Float = _

  var largeVolume: Float = _
  var largeAmount: Float = _

  var smallVolume: Float = _
  var smallAmount: Float = _
}
