/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities
import org.aiotrade.lib.securities.model.Sec
import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.dataserver.QuoteInfo
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.math.indicator.Plot
import scala.collection.mutable.HashMap

class InfoPointSer ($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
  val infos   = TVar[ArrayList[QuoteInfo]]("I", Plot.None)


  def updateFromNoFire(info : QuoteInfo) : TSerEvent = {
    val cal = Calendar.getInstance($sec.exchange.timeZone)

    val time = $freq.round(info.generalInfo.publishTime, cal)
    createWhenNonExist(time)
    infos(time) match {
      case null => val ifs = ArrayList[QuoteInfo]()
        ifs.append(info)
      case ifs => ifs.append(info)
    }
    TSerEvent.Updated(this, "", time, time)
  }

  def exportTo(fromTime : Long, toTime : Long) : List[HashMap[String, Any]] =  {
    try {
      readLock.lock
      timestamps.readLock.lock

      val vs = vars filter (v => v.name != "" && v.name != null)
      val frIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
      var toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
      toIdx = vs.foldLeft(toIdx){(acc, v) => math.min(acc, v.values.length)}
      val quoteInfos = for(i : Int <- frIdx to toIdx; quoteInfo <- infos(i)) yield quoteInfo.export

      quoteInfos.toList   
    } finally {
      timestamps.readLock.unlock
      readLock.unlock
    }
  }
  def updateFrom(info : QuoteInfo) {

    publish(updateFromNoFire(info))
  }


}
