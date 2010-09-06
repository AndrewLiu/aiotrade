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
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util.actors.Event
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.collection.ArrayList
import ru.circumflex.orm._

class QuoteInfo extends TVal {
  var generalInfo : GeneralInfo =  new GeneralInfo()
  var content : String = _
  var summary : String = _
  var categories : ListBuffer[ContentCategory] = new ListBuffer[ContentCategory]()
  var secs : ListBuffer[Sec] = new ListBuffer[Sec]()
  def export: HashMap[String, Any]= {
    HashMap[String, Any] ("publishTime" -> generalInfo.publishTime,
                          "title" -> generalInfo.title,
                          "url" -> generalInfo.url,
                          "combinValue" -> generalInfo.combinValue,
                          "content" -> content,
                          "summary" -> summary,
                          "category" -> {for(cate <- categories) yield cate.code},
                          "symbol" -> {for(sec <- secs) yield sec.uniSymbol})
  }
}

case class QuoteInfoSnapshot(publishTime : Long, title: String, url : String,
                             combinValue : Long, content : String, summary : String,
                             category : List[ContentCategory], secs : List[Sec] ) extends Event{

  def export: HashMap[String, Any]= {
    HashMap[String, Any] ("publishTime" -> publishTime,
                          "title" -> title,
                          "url" -> url,
                          "combinValue" -> combinValue,
                          "content" -> content,
                          "summary" -> summary,
                          "category" -> {for(cate <- category) yield cate.code},
                          "symbol" -> {for(sec <- secs) yield sec.uniSymbol})
  }
}

case class QuoteInfoSnapshots(events : List[QuoteInfoSnapshot]) extends Event {
  def export: List[HashMap[String, Any]] = for(event <- events) yield event.export
}

object QuoteInfoDataServer extends Publisher

abstract class QuoteInfoDataServer extends  DataServer[QuoteInfo] {
  type C = QuoteInfoContract
  private val log = Logger.getLogger(this.getClass.getName)

  private val updatedEvents = new ArrayList[TSerEvent]
  private val allQuoteInfo = new ArrayList[QuoteInfoSnapshot]

  refreshable = true
  
  protected def composeSer(values: Array[QuoteInfo]) : Seq[TSerEvent] = {
    updatedEvents.clear
    allQuoteInfo.clear
    count = 0

    for (info <- values ; sec <- info.secs) {
      sec.infoPointSerOf(TFreq.ONE_MIN) match {
        case Some(minuteSer) => val event = minuteSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
      sec.infoPointSerOf(TFreq.DAILY) match {
        case Some(dailyeSer) => val event = dailyeSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
      val category = info.categories.headOption match {
        case Some(x) => x
        case None => null
      }
      val quoteInfo = QuoteInfoSnapshot(info.generalInfo.publishTime, info.generalInfo.title,
                                        info.generalInfo.url, info.generalInfo.combinValue,
                                        info.content, info.summary,info.categories.toList,
                                        info.secs.toList)
      allQuoteInfo += quoteInfo
    }

    values foreach (value => GeneralInfo.save(value))
    COMMIT
    
    if (allQuoteInfo.length > 0) {
      QuoteInfoDataServer.publish(QuoteInfoSnapshots(allQuoteInfo.toList))
    }
    updatedEvents
  }

  override protected def postLoadHistory(values: Array[QuoteInfo]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case event@TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(event)
        log.info(symbol + ": " + count + ", data loaded, load QuoteInfo server finished")
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

  override protected def postRefresh(values: Array[QuoteInfo]): Long = {
    val events = composeSer(values)
    var lastTime = Long.MinValue
    events foreach {
      case event@TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(event)
        log.info(source + " publish event " + event)
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

}

