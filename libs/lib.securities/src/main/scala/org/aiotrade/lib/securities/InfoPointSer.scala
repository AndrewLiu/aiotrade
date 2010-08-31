/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.dataserver.QuoteInfo
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.math.indicator.Plot

class InfoPointSer ($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
  val title = TVar[ArrayList[QuoteInfo]]("I", Plot.Info)
}
