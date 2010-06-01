package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._

object MoneyFlows1d extends MoneyFlows {
  def currentDailyMoneyFlow(sec: Sec): MoneyFlow = synchronized {
    val cal = Calendar.getInstance(sec.exchange.timeZone)
    val now = TFreq.DAILY.round(System.currentTimeMillis, cal)

    (SELECT (MoneyFlows1d.*) FROM (MoneyFlows1d) WHERE (
        (MoneyFlows1d.sec.field EQ Secs.idOf(sec)) AND (MoneyFlows1d.time EQ now)
      ) unique
    ) match {
      case Some(mf) => mf
      case None =>
        val mf = new MoneyFlow
        mf.time = now
        mf.sec = sec
        mf.unclosed_! // @todo when to close it and update to db?
        MoneyFlows1d.save(mf)
        commit
        mf
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

  def closedMoneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ 1)
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
