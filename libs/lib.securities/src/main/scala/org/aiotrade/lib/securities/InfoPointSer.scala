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

class InfoPointSer ($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
    val infos = TVar[ArrayList[QuoteInfo]]("I", Plot.None)


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

    def updateFrom(info : QuoteInfo) {

    publish(updateFromNoFire(info))
  }


}
