/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities

import java.util.logging.Logger
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{TVar, TVal, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Sec

class FreeFloatSer(_sec: Sec, _freq: TFreq) extends DefaultBaseTSer(_sec, _freq)  {

  val freeFloat = TVar[Double]("FF", Plot.None)

  private val log = Logger.getLogger(getClass.getName)
  override def serProvider: Sec = super.serProvider.asInstanceOf[Sec]

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    val idx = this.indexOfOccurredTime(time) - 1
    if (idx >= 0) freeFloat(time) = freeFloat(idx)
  }

  protected def calcRateByFreeFloat(col: TVar[Double], volume: TVar[Double]){
    val infos = Exchanges.secInfosOf(serProvider)
    if (infos.isEmpty) {
      log.info("There is no secinfos of " + serProvider)
      return
    }

    val infoItr = infos.iterator
    var i = size
    while (infoItr.hasNext) {
      val info = infoItr.next
      var stop = false
      while({i -= 1; i >= 0 && !stop}){
        val time = timestamps(i)
        log.fine("Sec=" + info.uniSymbol + ",time = " + time + ", info.validFrom = " + info.validFrom + ", freefloat = " + info.freeFloat)
        if (time > info.validFrom && info.freeFloat > 0){
          col(time) = volume(time) / info.freeFloat
          freeFloat(time) = info.freeFloat
        }
        else{
          i += 1
          stop = true
        }
        log.fine("column (turnoverRate/netBuyPercent)=" + col(time) + ", volume =" + volume(time))
      }
    }
  }

}
