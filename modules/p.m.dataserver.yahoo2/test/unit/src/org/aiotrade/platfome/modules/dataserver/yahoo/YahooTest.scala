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
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.platform.modules.dataserver.yahoo._

class YahooTest extends TestHelper {

    @Before
    def setUp: Unit = {
    }

    @After
    def tearDown: Unit = {
    }

    @Test{val timeout=1800000}
    def example = {
        main("BP.L")
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
        
        loadSer(dailyContents)
        loadSer(rtContents)

        sec.subscribeTickerServer

        // wait for some seconds
        waitFor(10000)

        val dailyInds  = initIndicators(dailyContents, dailySer)
        val oneMinInds = initIndicators(rtContents, oneMinSer)
        dailyInds. foreach{x => computeAsync(x)}
        oneMinInds.foreach{x => computeAsync(x)}

        println("size of daily quote: " + dailySer.size)
        println("size of 1 min quote: " + oneMinSer.size)
        println("size of ticker ser: "  + tickerSer.size)

        dailyInds.foreach(printValuesOf(_))
        oneMinInds.foreach(printValuesOf(_))

        while (true) {
            waitFor(6000)

            println("size of daily quote: " + dailySer.size)
            println("size of 1 min quote: " + oneMinSer.size)
            println("size of ticker ser: "  + tickerSer.size)

            dailyInds.foreach(printLastValueOf(_))
            oneMinInds.foreach(printLastValueOf(_))
        }
    }

}
