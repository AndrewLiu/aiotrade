package org.aiotrade.platform.test.yahoo

import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.computable._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.model._
import org.aiotrade.lib.dataserver.yahoo._
import org.aiotrade.platform.test.StockCode
import org.aiotrade.platform.test.TestHelper
import scala.actors.Actor

object YahooQuoteServerTest extends TestHelper {

  def main(args: Array[String]) {
    testBatch
  }

  def testBatch : Unit = {
    val size = 5
    val syms = StockCode.SHSE.keySet
    val actors = new Array[TestOne](size)

    var i = 0
    val itr = syms.iterator
    while (i < size && itr.hasNext) {
      val sym = itr.next
      val actor = new TestOne(sym + ".SS")
      actors(i) = actor
      i += 1
    }

    while (true) {
      waitFor(5000)
      actors foreach {x =>
        reportQuote(x.sec)
        reportInds(x.oneMinInds)
        reportInds(x.dailyInds)
      }
    }
  }

  class TestOne(symbol:String) extends Actor {

    def act {}

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

    val dailySer  = sec.serOf(dailyFreq).get
    val oneMinSer = sec.serOf(oneMinFreq).get
    val tickerSer = sec.tickerSer

    // * init indicators before loadSer, so, they can receive the FinishedLoading evt
    val dailyInds  = initIndicators(dailyContents, dailySer)
    val oneMinInds = initIndicators(rtContents, oneMinSer)

    loadSer(dailyContents)
    //loadSer(rtContents)

    // wait for some secs for data loading
    //waitFor(10000)

    // * Here, we test two possible condiction:
    // * 1. inds may have been computed by FinishedLoading evt,
    // * 2. data loading may not finish yet
    // * For what ever condiction, we force to compute it again to test concurrent
    dailyInds  foreach {x => computeAsync(x)}
    oneMinInds foreach {x => computeAsync(x)}

    sec.subscribeTickerServer
  }

}
