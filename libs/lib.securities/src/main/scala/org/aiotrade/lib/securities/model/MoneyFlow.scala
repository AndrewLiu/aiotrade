package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._

object MoneyFlows1d extends MoneyFlows {
  def dailyMoneyFlowOf(sec: Sec, time: Long): MoneyFlow = synchronized {
    val exchange = sec.exchange
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)

    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ rounded)
      ) list
    ) headOption match {
      case Some(one) => one
      case None =>
        val newone = new MoneyFlow
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_! // @todo when to close it and update to db?
        newone.justOpen_!
        newone.fromMe_!
        MoneyFlows1d.save(newone)
        commit
        //exchange.addUnclosedDailyMoneyFlow(newone)
        newone
    }
  }

}

object MoneyFlows1m extends MoneyFlows

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
}
