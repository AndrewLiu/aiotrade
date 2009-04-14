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

    @Test{val timeout=30000}
    def example = {
        main("600373.SS")
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
        loadSer(dailyContents)

        val rtContents = createAnalysisContents(symbol, oneMinFreq)
        rtContents.addDescriptor(oneMinQuoteContract)
        rtContents.serProvider = sec
        loadSer(rtContents)

        sec.subscribeTickerServer

        // wait for some seconds
        waitFor(10000)

        sec.serOf(dailyFreq).foreach{x => println("size of daily quote: " + x.size)}
        computeIndicators(dailyContents, sec.serOf(dailyFreq).get)
    }

}
