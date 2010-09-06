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

    scala.actors.Actor.actor {
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
  }

  class TestOne(symbol:String) {

    val quoteServer  = classOf[YahooQuoteServer]
    val tickerServer = classOf[YahooTickerServer]

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

    val dailyContents = createAnalysisContents(symbol, dailyFreq)
    dailyContents.addDescriptor(dailyQuoteContract)
    dailyContents.serProvider = sec

    val rtContents = createAnalysisContents(symbol, oneMinFreq)
    rtContents.addDescriptor(oneMinQuoteContract)
    rtContents.serProvider = sec

    val weeklyContents = createAnalysisContents(symbol, TFreq.WEEKLY)
    //weeklyContents.addDescriptor(dailyQuoteContract)
    weeklyContents.serProvider = sec

    val daySer  = sec.serOf(dailyFreq).get
    val minSer = sec.serOf(oneMinFreq).get

    // * init indicators before loadSer, so, they can receive the Loaded evt
    val dailyInds  = initIndicators(dailyContents, daySer)
    val oneMinInds = initIndicators(rtContents, minSer)

    loadSer(dailyContents)
    //loadSer(rtContents)

    val weeklySer = sec.serOf(TFreq.WEEKLY).get
    val weeklyInds = initIndicators(weeklyContents, weeklySer)
    
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
