/*
 * PersistenceManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import org.aiotrade.lib.indicator.VOLIndicator
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.dataserver.QuoteServer
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.Quote
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.platform.modules.indicator.basic._
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
class PlainPersistenceManager extends PersistenceManager {

  private val quoteServers  = new ArrayBuffer[QuoteServer]
  private val tickerServers = new ArrayBuffer[TickerServer]
  private val indicators = new ArrayBuffer[Indicator]

  def saveQuotes(symbol: String, freq: TFreq, quotes: ArrayBuffer[Quote], sourceId: Long): Unit = {}
  def restoreQuotes(symbol: String, freq: TFreq): ArrayBuffer[Quote] = new ArrayBuffer[Quote]
  def deleteQuotes(symbol: String, freq: TFreq, fromTime: Long, toTime: Long): Unit = {}
  def dropAllQuoteTables(symbol: String): Unit = {}

  def shutdown: Unit = {}

  def restoreProperties: Unit = {}
  def saveProperties: Unit = {}

  def saveContents(contents: AnalysisContents): Unit = {}
  def restoreContents(symbol: String): AnalysisContents = new AnalysisContents(symbol)
  def defaultContents: AnalysisContents = new AnalysisContents("<Default>")

  def lookupAllRegisteredServices[T](tpe: Class[T], folderName: String) :Seq[T] = {
    if (tpe == classOf[QuoteServer]) {
      if (quoteServers.isEmpty) {
        //quoteServers += new YahooQuoteServer
      }
      quoteServers.asInstanceOf[Seq[T]]
    } else if (tpe == classOf[TickerServer]) {
      if (tickerServers.isEmpty) {
        //tickerServers += new YahooTickerServer
      }
      tickerServers.asInstanceOf[Seq[T]]
    } else if (tpe == classOf[Indicator]) {
      if (indicators.isEmpty) {
        indicators ++= List(new ARBRIndicator,
                            new BIASIndicator,
                            new BOLLIndicator,
                            new CCIIndicator,
                            new DMIIndicator,
                            new EMAIndicator,
                            new GMMAIndicator,
                            new HVDIndicator,
                            new KDIndicator,
                            new MACDIndicator,
                            new MAIndicator,
                            new MFIIndicator,
                            new MTMIndicator,
                            new OBVIndicator,
                            new ROCIndicator,
                            new RSIIndicator,
                            new SARIndicator,
                            new WMSIndicator,
                            new ZIGZAGFAIndicator,
                            new ZIGZAGIndicator
        )
      }
      indicators.asInstanceOf[Seq[T]]
    } else {
      Nil
    }
  }

}
