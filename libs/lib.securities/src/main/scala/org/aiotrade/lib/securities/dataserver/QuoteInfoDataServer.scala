/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.ContentCategory
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable.ListBuffer
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher

class QuoteInfo extends TVal {
  var generalInfo : GeneralInfo =  new GeneralInfo()
  var content : String = _
  var summery : String = _
  var categories : ListBuffer[ContentCategory] = new ListBuffer[ContentCategory]()
  var secs : ListBuffer[Sec] = new ListBuffer[Sec]()
}

case class QuoteInfoSnapshot(publishTime : Long, title: String, url : String,
                             combinValue : Long, content : String, summery : String,
                             category : ContentCategory, secs : List[Sec] ) extends Event

case class QuoteInfoSnapshots(events : Array[QuoteInfoSnapshot]) extends Event

object QuoteInfoDataServer extends Publisher

abstract class QuoteInfoDataServer extends  DataServer[QuoteInfo] {
  type C = QuoteInfoContract
  private val log = Logger.getLogger(this.getClass.getName)

  refreshable = true
  
  protected def composeSer(values: Array[QuoteInfo]) : Seq[TSerEvent] = {
    for (info <- values ; sec <- info.secs) {
      sec.infoPointSerOf(TFreq.ONE_MIN) match {
        case Some(minuteSer) =>
        case _ =>
      }
      sec.infoPointSerOf(TFreq.DAILY) match {
        case Some(minuteSer) =>
        case _ =>
      }
    }
    null
  }

  override protected def postLoadHistory(values: Array[QuoteInfo]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(TSerEvent.FinishedLoading(source, symbol, fromTime, toTime, lastObject, callback))
        log.info(symbol + ": " + count + ", data loaded, load server finished")
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

  override protected def postRefresh(values: Array[QuoteInfo]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case TSerEvent.ToBeSet(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback))
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

}

