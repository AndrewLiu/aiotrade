/*
 * Test.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert._

import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.computable._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.platform.modules.dataserver.yahoo._
import scala.actors.Actor

class YahooTest extends TestHelper {

    @Before
    def setUp: Unit = {
    }

    @After
    def tearDown: Unit = {
    }

    @Test{val timeout=1800000}
    def example = {
        testBatch
    }


    def testBatch : Unit = {
        val size = 10
        val syms = StockCode.SHSE.keySet
        val actors = new Array[TestOne](size)

        var i = 0
        val itr = syms.elements
        while (i < size && itr.hasNext) {
            val sym = itr.next
            val actor = new TestOne(sym + ".SS")
            actors(i) = actor
            i += 1
        }

        while (true) {
            waitFor(5000)
            actors.foreach{x =>
                reportQuote(x.sec)
                reportInds(x.oneMinInds)
                reportInds(x.dailyInds)
            }
        }
    }


    def main(symbol:String) {
        val quoteServer  = classOf[YahooQuoteServer]
        val tickerServer = classOf[YahooTickerServer]

        val oneMinFreq = Frequency.ONE_MIN
        val dailyFreq = Frequency.DAILY

        val dailyQuoteContract = createQuoteContract(symbol, "", "", dailyFreq, false, quoteServer)

        val supportOneMin = dailyQuoteContract.isFreqSupported(oneMinFreq)

        val oneMinQuoteContract = createQuoteContract(symbol, "", "", oneMinFreq, false, quoteServer)
        val tickerContract = createTickerContract(symbol, "", "", oneMinFreq, tickerServer)

        val quoteContracts = List(dailyQuoteContract, oneMinQuoteContract)

        val sec = new Stock(symbol, quoteContracts, tickerContract)
        val market = YahooQuoteServer.marketOf(symbol)
        sec.market = market

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
        loadSer(rtContents)

        sec.subscribeTickerServer

        // wait for some secs for data loading
        //waitFor(10000)

        // * Here, we test two possible condiction:
        // * 1. inds may have been computed by FinishedLoading evt, 
        // * 2. data loading may not finish yet
        // * For what ever condiction, we force to compute it again to test concurrent
        dailyInds. foreach{x => computeAsync(x)}
        oneMinInds.foreach{x => computeAsync(x)}

        println("==================")
        println("size of daily quote: " + dailySer.size)
        println("size of 1 min quote: " + oneMinSer.size)
        println("size of ticker ser: "  + tickerSer.size)

        dailyInds.foreach(printValuesOf(_))
        oneMinInds.foreach(printValuesOf(_))

        while (true) {
            waitFor(6000)

            println("==================")
            println("size of daily quote: " + dailySer.size)
            println("size of 1 min quote: " + oneMinSer.size)
            println("size of ticker ser: "  + tickerSer.size)

            dailyInds.foreach(printLastValueOf(_))
            oneMinInds.foreach(printLastValueOf(_))
        }
    }

    class TestOne(symbol:String) extends Actor {

        def act {}

        val quoteServer  = classOf[YahooQuoteServer]
        val tickerServer = classOf[YahooTickerServer]

        val oneMinFreq = Frequency.ONE_MIN
        val dailyFreq = Frequency.DAILY

        val dailyQuoteContract = createQuoteContract(symbol, "", "", dailyFreq, false, quoteServer)

        val supportOneMin = dailyQuoteContract.isFreqSupported(oneMinFreq)

        val oneMinQuoteContract = createQuoteContract(symbol, "", "", oneMinFreq, false, quoteServer)
        val tickerContract = createTickerContract(symbol, "", "", oneMinFreq, tickerServer)

        val quoteContracts = List(dailyQuoteContract, oneMinQuoteContract)

        val sec = new Stock(symbol, quoteContracts, tickerContract)
        val market = YahooQuoteServer.marketOf(symbol)
        sec.market = market

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
        dailyInds. foreach{x => computeAsync(x)}
        //oneMinInds.foreach{x => computeAsync(x)}

        //sec.subscribeTickerServer
    }

    def reportQuote(sec:Stock) {
        sec.uniSymbol
        println("\n======= " + new java.util.Date + " size of " + sec.uniSymbol  + " ======")
        sec.serOf(Frequency.DAILY).  foreach{x => println("daily: "  + x.size)}
        sec.serOf(Frequency.ONE_MIN).foreach{x => println("1 Min: "  + x.size)}
        println("ticker: "  + sec.tickerSer.size)
    }

    def reportInds(inds:Seq[Indicator]) {
        inds.foreach{printLastValueOf(_)}
    }



}
