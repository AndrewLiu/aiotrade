package org.aiotrade.platform.test.yahoo

import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.model._
import java.util.Timer
import java.util.TimerTask
import org.aiotrade.lib.dataserver.yahoo._
import org.aiotrade.platform.test.TestHelper
import scala.collection.mutable.ArrayBuffer

object YahooQuoteServerTest extends TestHelper {

  def main(args: Array[String]) {
    testBatch
  }

  def testBatch {
    val size = 5
    val syms = Exchange.symbolsOf(Exchange.SS)
    val testers = new ArrayBuffer[TestOne]

    var i = 0
    val itr = syms.iterator
    while (i < size && itr.hasNext) {
      val sym = itr.next
      val tester = new TestOne(sym)
      testers += tester
      i += 1
    }

    val timer = new Timer
    timer.schedule(new TimerTask {
        def run {
          testers foreach {x =>
            reportQuote(x.sec)
            reportInds(x.oneMinInds)
            reportInds(x.dailyInds)
            reportInds(x.weeklyInds)
          }
        }
      }, 5000, 6000)
  }

  class TestOne(symbol:String) {

    val quoteServer  = YahooQuoteServer.getClass.getName
    val tickerServer = YahooTickerServer.getClass.getName

    val oneMinFreq = TFreq.ONE_MIN
    val dailyFreq = TFreq.DAILY

    val dailyQuoteContract = createQuoteContract(symbol, "", "", dailyFreq, false, quoteServer)

    val supportOneMin = dailyQuoteContract.isFreqSupported(oneMinFreq)

    val oneMinQuoteContract = createQuoteContract(symbol, "", "", oneMinFreq, false, quoteServer)
    val tickerContract = createTickerContract(symbol, "", "", oneMinFreq, tickerServer)

    val quoteContracts = List(dailyQuoteContract, oneMinQuoteContract)

    val sec = Exchange.secOf(symbol).get
    sec.quoteContracts = quoteContracts
    sec.tickerContract = tickerContract
    val exchange = YahooQuoteServer.exchangeOf(symbol)
    sec.exchange = exchange

    val dailyContent = createContent(symbol, dailyFreq)
    dailyContent.addDescriptor(dailyQuoteContract)
    dailyContent.serProvider = sec

    val rtContent = createContent(symbol, oneMinFreq)
    rtContent.addDescriptor(oneMinQuoteContract)
    rtContent.serProvider = sec

    val weeklyContent = createContent(symbol, TFreq.WEEKLY)
    //weeklyContent.addDescriptor(dailyQuoteContract)
    weeklyContent.serProvider = sec

    val daySer  = sec.serOf(dailyFreq).get
    val minSer = sec.serOf(oneMinFreq).get

    // * init indicators before loadSer, so, they can receive the Loaded evt
    val dailyInds  = initIndicators(dailyContent, daySer)
    val oneMinInds = initIndicators(rtContent, minSer)

    loadSer(dailyContent)
    //loadSer(rtContent)

    val weeklySer = sec.serOf(TFreq.WEEKLY).get
    val weeklyInds = initIndicators(weeklyContent, weeklySer)
    
    // wait for some secs for data loading
    //waitFor(10000)

    // * Here, we test two possible condiction:
    // * 1. inds may have been computed by FinishedLoading evt,
    // * 2. data loading may not finish yet
    // * For what ever condiction, we force to compute it again to test concurrent
    dailyInds  foreach {x => computeAsync(x)}
    oneMinInds foreach {x => computeAsync(x)}
    weeklyInds foreach {x => computeAsync(x)}

    sec.subscribeTickerServer()
  }

}
