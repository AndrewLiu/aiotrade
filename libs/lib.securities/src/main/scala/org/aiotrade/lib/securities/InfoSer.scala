package org.aiotrade.lib.securities

import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.securities.model.Info
import org.aiotrade.lib.securities.model.Sec


object InfoSer {
  def main(args: Array[String]) {
    val nDays = 100
    val oneday = 24 * 60 * 60 * 1000
    val cal = Calendar.getInstance
    cal.add(Calendar.DAY_OF_YEAR, -nDays)

    val values = new ArrayList[Info]

    var i = 0
    while (i < nDays) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
      val info = new Info
      info.time = cal.getTimeInMillis
      info.infos = List("ABC", "DEF")
      values += info
      i += 1
    }

    val ser = new InfoSer(null, TFreq.DAILY)

    ser ++= values.toArray
    println(ser)

  }
}

class InfoSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {

  val infos = TVar[List[String]]("I", Plot.Info)


  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case info: Info =>
        infos(time) = info.infos
      case _ => assert(false, "Should pass a Info type TimeValue")
    }
  }

}
