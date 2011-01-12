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
import scala.collection.mutable
import java.util.logging.Logger

class InfoPointSer ($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
  val infos   = TVar[ArrayList[QuoteInfo]]("I", Plot.None)
  private val log = Logger.getLogger(this.getClass.getName)

  def updateFromNoFire(info : QuoteInfo) : TSerEvent = {
    try {
      writeLock.lock
      val cal = Calendar.getInstance($sec.exchange.timeZone)
      //log.info(("publishtime:" + info.generalInfo.publishTime + " title : " + info.generalInfo.title))
      val time = $freq.round(info.generalInfo.publishTime, cal)
      createWhenNonExist(time)
      Option(infos(time)) match {
        case Some(x) => x += info
        case None =>
          val ifs = ArrayList[QuoteInfo]()
          ifs.append(info)
          infos(time) = ifs

      }
      TSerEvent.Updated(this, "", time, time)
    } finally {
      writeLock.unlock
    }
  }

  def exportTo(fromTime : Long, toTime : Long) : List[mutable.Map[String, Any]] =  {
    try {
      readLock.lock
      timestamps.readLock.lock

      val vs = vars filter (v => v.name != "" && v.name != null)
      val frIdx = timestamps.indexOfNearestOccurredTimeBehind(fromTime)
      var toIdx = timestamps.indexOfNearestOccurredTimeBefore(toTime)
      toIdx = vs.foldLeft(toIdx){(acc, v) => math.min(acc, v.values.length)}
      val quoteInfos = ArrayList[mutable.Map[String, Any]]()
      for(i : Int <- 0 to infos.size) {
        if(infos(i) != null ){
          for (quoteInfo <- infos(i)) {
            if(quoteInfo != null) quoteInfos.append(quoteInfo.export)
          }
        }
      }
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
