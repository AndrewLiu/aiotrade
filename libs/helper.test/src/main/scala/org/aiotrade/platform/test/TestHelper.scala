/*
 * TestHelper.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import java.util.concurrent.TimeUnit
import org.aiotrade.lib.indicator.VOLIndicator
import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.computable._
import org.aiotrade.lib.math.timeseries.datasource._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.dataserver._
import org.aiotrade.lib.indicator.basic._

trait TestHelper {
    
  def createQuoteContract(symbol: String, category: String , sname: String, freq: TFreq, refreshable: Boolean, server: Class[_]): QuoteContract = {
    val dataContract = new QuoteContract

    dataContract.active = true
    dataContract.serviceClassName = server.getName

    dataContract.symbol = symbol
    dataContract.category = category
    dataContract.shortName = sname
    dataContract.secType = Security.Type.Stock
    dataContract.exchange = "SSH"
    dataContract.primaryExchange = "SSH"
    dataContract.currency = "USD"

    dataContract.freq = freq
    dataContract.refreshable = refreshable
    dataContract.refreshInterval = 5

    dataContract
  }

  def createTickerContract(symbol: String, category: String, sname: String, freq: TFreq, server: Class[_]): TickerContract = {
    val dataContract = new TickerContract

    dataContract.active = true
    dataContract.serviceClassName = server.getName

    dataContract.symbol = symbol
    dataContract.category = category
    dataContract.shortName = sname
    dataContract.secType = Security.Type.Stock
    dataContract.exchange = "SSH"
    dataContract.primaryExchange = "SSH"
    dataContract.currency = "USD"

    dataContract.freq = freq
    dataContract.refreshable = true
    dataContract.refreshInterval = 5

    dataContract
  }

  def createAnalysisContents(symbol: String, freq: TFreq): AnalysisContents = {
    val contents = new AnalysisContents(symbol)

    contents.addDescriptor(createIndicatorDescriptor(classOf[ARBRIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[BIASIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[BOLLIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[CCIIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[DMIIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[EMAIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[GMMAIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[HVDIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[KDIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[MACDIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[MAIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[MFIIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[MTMIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[OBVIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[ROCIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[RSIIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[SARIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[WMSIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[ZIGZAGFAIndicator], freq))
    contents.addDescriptor(createIndicatorDescriptor(classOf[ZIGZAGIndicator], freq))

    contents
  }

  def createIndicatorDescriptor[T <: Indicator](clazz: Class[T], freq: TFreq): IndicatorDescriptor = {
    val descriptor = new IndicatorDescriptor
    descriptor.active = true
    descriptor.serviceClassName = clazz.getName
    descriptor.freq = freq
    descriptor
  }


  def loadSer(contents: AnalysisContents): Unit = {
    val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]) getOrElse (return)

    val freq = quoteContract.freq
    if (!quoteContract.isFreqSupported(freq)) {
      return
    }

    val sec = contents.serProvider
    var mayNeedsReload = false
    if (sec == null) {
      return
    } else {
      mayNeedsReload = true
    }

    if (mayNeedsReload) {
      sec.clearSer(freq)
    }

    if (!sec.isSerLoaded(freq) && !sec.isSerInLoading(freq)) {
      sec.loadSer(freq)
    }
  }

  def initIndicators(contents: AnalysisContents, masterSer: MasterTSer): Seq[Indicator] = {
    var indicators: List[Indicator] = Nil
    for (descriptor <- contents.lookupDescriptors(classOf[IndicatorDescriptor])
         if descriptor.active && descriptor.freq.equals(masterSer.freq)) yield {

      descriptor.serviceInstance(masterSer) match {
        case None => println("In test: can not init instance of: " + descriptor.serviceClassName)
        case Some(indicator) => indicators ::= indicator
      }
    }
    indicators
  }

  def computeSync(indicator: Indicator): Unit = {
    indicator match {
      case _: SpotComputable => // don't compute it right now
      case _ =>
        val t0 = System.currentTimeMillis
        indicator.computeFrom(0)
        println("Computing " + indicator.shortDescription + "(" + indicator.freq + ", size=" + indicator.size +  "): " + (System.currentTimeMillis - t0) + " ms")
    }
  }

  def computeAsync(indicator: Indicator): Unit = {
    indicator match {
      case _: SpotComputable => // don't compute it right now
      case _ => indicator.computableActor ! ComputeFrom(0)
    }
  }

  def printValuesOf(indicator: Indicator): Unit = {
    println
    println(indicator.freq)
    println(indicator.shortDescription + ":" + indicator.size)
    for (v <- indicator.vars) {
      print(v.name + ": ")
      v.values.reverse foreach {x => print(x + ",")}
      println
    }
  }

  def printLastValueOf(indicator: Indicator): Unit = {
    println
    println(indicator.freq + "-" +indicator.shortDescription + ":" + indicator.size)
    for (v <- indicator.vars if v.size > 0) {
      println(v.name + ": " + v.values.last)
    }
  }

  def reportQuote(sec: Stock) {
    sec.uniSymbol
    println("\n======= " + new java.util.Date + " size of " + sec.uniSymbol  + " ======")
    sec.serOf(TFreq.DAILY)   foreach {x => println("daily: "  + x.size)}
    sec.serOf(TFreq.ONE_MIN) foreach {x => println("1 min: "  + x.size)}
    println("ticker: "  + sec.tickerSer.size)
  }

  def reportInds(inds: Seq[Indicator]) {
    inds foreach printLastValueOf
  }


  // wait for some ms
  def waitFor(ms: Long): Unit = {
    TimeUnit.MILLISECONDS.sleep(ms)
  }

}
